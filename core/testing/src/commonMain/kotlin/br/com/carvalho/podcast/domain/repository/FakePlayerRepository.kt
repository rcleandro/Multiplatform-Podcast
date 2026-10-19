package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Bookmark
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.ListeningStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePlayerRepository : PlayerRepository {
    var savedPlaybackState: PlaybackState? = null
    var saveCount = 0
        private set

    /** Every episode's bookmarks, in the order they were added. */
    val savedBookmarks = MutableStateFlow<List<Bookmark>>(emptyList())

    override suspend fun savePlaybackState(
        episodeId: String?,
        position: Long,
        speed: Float,
        queue: List<Episode>,
        queueUpdatedAt: Long,
    ) {
        saveCount++
        savedPlaybackState = PlaybackState(episodeId, position, speed, queue, queueUpdatedAt)
    }

    override suspend fun getSavedPlaybackState(): PlaybackState? = savedPlaybackState

    override fun bookmarks(episodeId: String): Flow<List<Bookmark>> = savedBookmarks.map { all ->
        all.filter { it.episodeId == episodeId }.sortedBy { it.positionMs }
    }

    override suspend fun addBookmark(episodeId: String, positionMs: Long, note: String) {
        val id = (savedBookmarks.value.maxOfOrNull { it.id } ?: 0) + 1
        savedBookmarks.value += Bookmark(id, episodeId, positionMs, note.trim(), createdAt = 0)
    }

    override suspend fun allBookmarks(): List<Bookmark> = savedBookmarks.value

    /** Each call to [addListening]: podcast, listened and saved. */
    val listened = mutableListOf<Triple<String, Long, Long>>()
    val stats = MutableStateFlow(ListeningStats())

    override suspend fun addListening(podcastId: String, at: Long, listenedMs: Long, savedMs: Long) {
        listened += Triple(podcastId, listenedMs, savedMs)
    }

    override fun listeningStats(): Flow<ListeningStats> = stats

    override suspend fun deleteBookmark(id: Long) {
        savedBookmarks.value = savedBookmarks.value.filterNot { it.id == id }
    }
}
