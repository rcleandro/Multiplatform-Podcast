package br.com.carvalho.podcast.domain.player

import br.com.carvalho.podcast.core.player.setupMediaSessionActions
import br.com.carvalho.podcast.core.player.updateMediaSessionMetadata
import br.com.carvalho.podcast.core.player.updateMediaSessionPlaybackState
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.core.util.AppLogger
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.HTMLAudioElement

private const val TAG = "AudioPlayer"

@OptIn(ExperimentalWasmJsInterop::class)
class WasmAudioPlayer : AudioPlayer {
    private val audio = (window.document.createElement("audio") as HTMLAudioElement).apply {
        onplay = {
            playbackRate = _playerState.value.speed.toDouble()
            updateState(playing = true, buffering = false)
        }
        onpause = { updateState(playing = false, buffering = false) }
        onwaiting = { updateState(buffering = true) }
        onplaying = { updateState(buffering = false) }
        onended = {
            updateState(playing = false, buffering = false)
            playNext()
        }
        onloadedmetadata = {
            updateState(duration = (duration * 1000).toLong())
        }
    }

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    init {
        setupMediaSessionActions(
            onPlay = { resume() },
            onPause = { pause() },
            onSeekBackward = { skipBackward(15) },
            onSeekForward = { skipForward(30) },
            onPreviousTrack = { playPrevious() },
            onNextTrack = { playNext() }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun play(episode: Episode) {
        val playbackUri = episode.localPath ?: episode.audioUrl
        AppLogger.i(TAG, "Playing episode in Wasm: ${episode.title} ($playbackUri)")

        updateMediaSessionMetadata(
            title = episode.title,
            artist = episode.podcastTitle.orEmpty(),
            artworkUrl = episode.imageUrl.orEmpty()
        )

        audio.src = playbackUri
        try {
            audio.play()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error playing audio in Wasm", e)
        }
        _playerState.value = _playerState.value.copy(currentEpisode = episode)
        startProgressUpdate()
    }

    override fun prepare(episode: Episode, positionMs: Long) {
        val playbackUri = episode.localPath ?: episode.audioUrl

        updateMediaSessionMetadata(
            title = episode.title,
            artist = episode.podcastTitle.orEmpty(),
            artworkUrl = episode.imageUrl.orEmpty()
        )

        audio.src = playbackUri
        audio.currentTime = positionMs / 1000.0
        _playerState.value = _playerState.value.copy(
            currentEpisode = episode,
            position = positionMs,
            duration = episode.duration * 1000
        )
    }

    override fun pause() {
        setSpeed(1.0f)
        audio.pause()
        stopProgressUpdate()
    }

    override fun resume() {
        audio.play()
        startProgressUpdate()
    }

    override fun stop() {
        audio.pause()
        audio.currentTime = 0.0
        stopProgressUpdate()
    }

    override fun seekTo(positionMs: Long) {
        audio.currentTime = positionMs / 1000.0
    }

    override fun setSpeed(speed: Float) {
        if (audio.paused) return
        _playerState.value = _playerState.value.copy(speed = speed)
        audio.playbackRate = speed.toDouble()
    }

    override fun skipForward(seconds: Int) {
        audio.currentTime += seconds.toDouble()
    }

    override fun skipBackward(seconds: Int) {
        audio.currentTime -= seconds.toDouble()
    }

    override fun setSleepTimer(millis: Long?, selectedMinutes: Int?) {
        _playerState.value = _playerState.value.copy(
            sleepTimerMillis = millis,
            selectedSleepTimerMinutes = selectedMinutes
        )
    }

    override fun setQueue(episodes: List<Episode>) {
        _playerState.value = _playerState.value.copy(queue = episodes)
    }

    override fun playNext() {
        val state = _playerState.value
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentEpisode?.id }
        if (currentIndex != -1 && currentIndex < state.queue.size - 1) {
            val nextEpisode = state.queue[currentIndex + 1]
            scope.launch { play(nextEpisode) }
        } else {
            stop()
        }
    }

    override fun playPrevious() {
        val state = _playerState.value
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentEpisode?.id }
        if (currentIndex > 0) {
            val prevEpisode = state.queue[currentIndex - 1]
            scope.launch { play(prevEpisode) }
        } else {
            seekTo(0)
        }
    }

    override fun release() {
        stopProgressUpdate()
        audio.pause()
        scope.cancel()
    }

    private fun updateState(
        playing: Boolean = _playerState.value.isPlaying,
        duration: Long = _playerState.value.duration ?: 0,
        buffering: Boolean = _playerState.value.isBuffering
    ) {
        _playerState.value = _playerState.value.copy(
            isPlaying = playing,
            duration = duration,
            isBuffering = buffering
        )
        updateMediaSessionPlaybackState(if (playing) "playing" else "paused")
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val current = audio.currentTime
                val total = audio.duration
                if (!total.isNaN()) {
                    _playerState.value = _playerState.value.copy(
                        position = (current * 1000).toLong(),
                        duration = (total * 1000).toLong()
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
}

actual fun createAudioPlayer(): AudioPlayer = WasmAudioPlayer()
