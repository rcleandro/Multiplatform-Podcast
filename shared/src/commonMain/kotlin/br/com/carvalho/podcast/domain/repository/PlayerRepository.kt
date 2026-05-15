package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Episode

interface PlayerRepository {
    suspend fun savePlaybackState(episodeId: String?, position: Long, speed: Float, queue: List<Episode>)
    suspend fun getSavedPlaybackState(): PlaybackState?
}

data class PlaybackState(
    val episodeId: String?,
    val position: Long,
    val speed: Float,
    val queue: List<Episode>
)
