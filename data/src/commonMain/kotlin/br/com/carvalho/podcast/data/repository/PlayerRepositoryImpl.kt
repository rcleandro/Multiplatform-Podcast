package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.data.local.dao.PlaybackStateDao
import br.com.carvalho.podcast.data.local.entity.PlaybackStateEntity
import br.com.carvalho.podcast.data.mapper.toDomain
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.PlaybackState
import br.com.carvalho.podcast.domain.repository.PlayerRepository

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
}
