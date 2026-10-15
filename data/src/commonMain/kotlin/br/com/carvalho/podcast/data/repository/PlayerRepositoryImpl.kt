package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.data.local.dao.PlaybackStateDao
import br.com.carvalho.podcast.data.local.entity.BookmarkEntity
import br.com.carvalho.podcast.data.local.entity.PlaybackStateEntity
import br.com.carvalho.podcast.data.mapper.toDomain
import br.com.carvalho.podcast.domain.model.Bookmark
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.PlaybackState
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayerRepositoryImpl(private val playbackStateDao: PlaybackStateDao) : PlayerRepository {

    /** The queue last written; progress is saved every few seconds, the queue only when it changes. */
    private var savedQueueIds: List<String>? = null

    override suspend fun savePlaybackState(episodeId: String?, position: Long, speed: Float, queue: List<Episode>) {
        playbackStateDao.save(PlaybackStateEntity(episodeId = episodeId, position = position, speed = speed))
        val queueIds = queue.map { it.id }
        if (queueIds != savedQueueIds) {
            playbackStateDao.replaceQueue(queueIds)
            savedQueueIds = queueIds
        }
    }

    override suspend fun getSavedPlaybackState(): PlaybackState? {
        val state = playbackStateDao.get() ?: return null
        return PlaybackState(
            episodeId = state.episodeId,
            position = state.position,
            speed = state.speed,
            queue = playbackStateDao.getQueue().map { it.toDomain() },
        )
    }

    override fun bookmarks(episodeId: String): Flow<List<Bookmark>> =
        playbackStateDao.bookmarks(episodeId).map { rows ->
            rows.map { Bookmark(it.id, it.episodeId, it.positionMs, it.note, it.createdAt) }
        }

    override suspend fun addBookmark(episodeId: String, positionMs: Long, note: String) {
        val bookmark = BookmarkEntity(
            episodeId = episodeId,
            positionMs = positionMs,
            note = note.trim(),
            createdAt = getCurrentTimestamp(),
        )
        playbackStateDao.insertBookmark(bookmark)
    }

    override suspend fun deleteBookmark(id: Long) = playbackStateDao.deleteBookmark(id)
}
