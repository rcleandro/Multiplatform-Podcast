package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Bookmark
import br.com.carvalho.podcast.domain.model.Episode
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    suspend fun savePlaybackState(episodeId: String?, position: Long, speed: Float, queue: List<Episode>)
    suspend fun getSavedPlaybackState(): PlaybackState?

    /** The episode's bookmarks (19.3), in the order they come in the episode. */
    fun bookmarks(episodeId: String): Flow<List<Bookmark>>

    /** [note] may be blank: a moment worth coming back to needs no words. */
    suspend fun addBookmark(episodeId: String, positionMs: Long, note: String)
    suspend fun deleteBookmark(id: Long)

    /** Every episode's bookmarks, for the backup (19.11). */
    suspend fun allBookmarks(): List<Bookmark>
}

data class PlaybackState(
    val episodeId: String?,
    val position: Long,
    val speed: Float,
    val queue: List<Episode>
)
