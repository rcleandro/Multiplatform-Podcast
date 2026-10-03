package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Episode

class FakePlayerRepository : PlayerRepository {
    var savedPlaybackState: PlaybackState? = null

    override suspend fun savePlaybackState(episodeId: String?, position: Long, speed: Float, queue: List<Episode>) {
        savedPlaybackState = PlaybackState(episodeId, position, speed, queue)
    }

    override suspend fun getSavedPlaybackState(): PlaybackState? = savedPlaybackState
}
