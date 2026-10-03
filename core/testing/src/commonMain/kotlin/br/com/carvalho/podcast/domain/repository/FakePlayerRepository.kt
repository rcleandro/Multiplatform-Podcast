package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Episode

class FakePlayerRepository : PlayerRepository {
    var savedPlaybackState: PlaybackState? = null
    var saveCount = 0
        private set

    override suspend fun savePlaybackState(episodeId: String?, position: Long, speed: Float, queue: List<Episode>) {
        saveCount++
        savedPlaybackState = PlaybackState(episodeId, position, speed, queue)
    }

    override suspend fun getSavedPlaybackState(): PlaybackState? = savedPlaybackState
}
