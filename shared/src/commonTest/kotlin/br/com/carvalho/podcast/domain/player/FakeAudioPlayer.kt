package br.com.carvalho.podcast.domain.player

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAudioPlayer : AudioPlayer {
    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _isReady = MutableStateFlow(true)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    var playCalledWith: Episode? = null
    var pauseCalled = false
    var resumeCalled = false
    var stopCalled = false
    var seekToCalledWith: Long? = null
    var queueSet: List<Episode>? = null

    override suspend fun play(episode: Episode) {
        playCalledWith = episode
        _playerState.value = _playerState.value.copy(currentEpisode = episode, isPlaying = true)
    }

    override fun prepare(episode: Episode, positionMs: Long) {
        _playerState.value = _playerState.value.copy(currentEpisode = episode, position = positionMs)
    }

    override fun pause() {
        pauseCalled = true
        _playerState.value = _playerState.value.copy(isPlaying = false)
    }

    override fun resume() {
        resumeCalled = true
        _playerState.value = _playerState.value.copy(isPlaying = true)
    }

    override fun stop() {
        stopCalled = true
        _playerState.value = _playerState.value.copy(isPlaying = false, currentEpisode = null)
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

    override fun setSleepTimer(millis: Long?, selectedMinutes: Int?) {
        _playerState.value = _playerState.value.copy(sleepTimerMillis = millis, selectedSleepTimerMinutes = selectedMinutes)
    }

    override fun setQueue(episodes: List<Episode>) {
        queueSet = episodes
        _playerState.value = _playerState.value.copy(queue = episodes)
    }

    override fun playNext() {}

    override fun playPrevious() {}

    override fun release() {}
}
