package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.data.local.dao.PlaybackStateDao
import br.com.carvalho.podcast.data.local.entity.PlaybackStateEntity
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.PlaybackState
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import kotlinx.serialization.json.Json

private const val TAG = "PlayerRepositoryImpl"

class PlayerRepositoryImpl(
    private val playbackStateDao: PlaybackStateDao,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : PlayerRepository {

    override suspend fun savePlaybackState(
        episodeId: String?,
        position: Long,
        speed: Float,
        queue: List<Episode>
    ) {
        val queueJson = json.encodeToString(queue)
        playbackStateDao.save(
            PlaybackStateEntity(
                episodeId = episodeId,
                position = position,
                speed = speed,
                queueJson = queueJson
            )
        )
    }

    override suspend fun getSavedPlaybackState(): PlaybackState? {
        val entity = playbackStateDao.get() ?: return null
        return try {
            val queue = json.decodeFromString<List<Episode>>(entity.queueJson)
            PlaybackState(
                episodeId = entity.episodeId,
                position = entity.position,
                speed = entity.speed,
                queue = queue
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to deserialize playback state queue. episodeId=${entity.episodeId}, queueJson='${entity.queueJson}'", e)
            null
        }
    }
}
