package br.com.carvalho.podcast.domain.player

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAudioPlayer : AudioPlayer {
    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()


    var playCalledWith: Episode? = null
    var pauseCalled = false
    var resumeCalled = false
    var seekToCalledWith: Long? = null
    var queueSet: List<Episode>? = null

    /** Moves the playback position, as the real players do every 500 ms while playing. */
    fun advanceTo(positionMs: Long) {
        _playerState.value = _playerState.value.copy(position = positionMs)
    }

    override suspend fun play(episode: Episode) {
        playCalledWith = episode
        _playerState.value = _playerState.value.copy(currentEpisode = episode, isPlaying = true)
    }


    override fun pause() {
        pauseCalled = true
        _playerState.value = _playerState.value.copy(isPlaying = false)
    }

    override fun resume() {
        resumeCalled = true
        _playerState.value = _playerState.value.copy(isPlaying = true)
    }


    override fun seekTo(positionMs: Long) {
        seekToCalledWith = positionMs
        _playerState.value = _playerState.value.copy(position = positionMs)
    }

    override fun setSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(speed = speed)
    }

    override fun skipForward(seconds: Int) {
        val current = _playerState.value.position
        _playerState.value = _playerState.value.copy(position = current + seconds * 1000)
    }

    override fun skipBackward(seconds: Int) {
        val current = _playerState.value.position
        _playerState.value = _playerState.value.copy(position = current - seconds * 1000)
    }

    override fun setSleepTimer(timer: SleepTimer?) {
        _playerState.value = _playerState.value.copy(sleepTimer = timer)
    }

    override fun setQueue(episodes: List<Episode>) {
        queueSet = episodes
        _playerState.value = _playerState.value.copy(queue = episodes)
    }

    override fun playNext() {}

    override fun playPrevious() {}

}
