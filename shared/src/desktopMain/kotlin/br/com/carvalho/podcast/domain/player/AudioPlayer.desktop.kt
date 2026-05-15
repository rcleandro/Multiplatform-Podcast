package br.com.carvalho.podcast.domain.player

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.core.util.AppLogger
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AudioPlayer"

actual class AudioPlayer actual constructor() {
    private var mediaPlayer: MediaPlayer? = null
    private val _playerState = MutableStateFlow(PlayerState())
    actual val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    actual val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var progressJob: Job? = null
    private val isInitialized = AtomicBoolean(false)

    init {
        initializeJavaFX()
        _isReady.value = true
    }

    private fun initializeJavaFX() {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                AppLogger.d(TAG, "Starting JavaFX Toolkit...")
                com.sun.javafx.application.PlatformImpl.startup {}
            } catch (e: Throwable) {
                AppLogger.e(TAG, "Failed to initialize JavaFX Toolkit", e)
            }
        }
    }

    private fun internalPrepare(episode: Episode, positionMs: Long, autoPlay: Boolean) {
        try {
            if (mediaPlayer != null && _playerState.value.currentEpisode?.id == episode.id) {
                if (autoPlay) mediaPlayer?.play()
                return
            }

            stop()
            updateState(buffering = true)
            AppLogger.i(TAG, "Preparing episode in Desktop: ${episode.title}")

            val playbackUri = if (episode.localPath != null) {
                java.io.File(episode.localPath).toURI().toString()
            } else {
                episode.audioUrl
            }
            val media = Media(playbackUri)
            val player = MediaPlayer(media)
            mediaPlayer = player

            player.onPlaying = Runnable {
                AppLogger.d(TAG, "Desktop player is playing")
                updateState(playing = true, buffering = false)
                player.rate = _playerState.value.speed.toDouble()
                startProgressUpdate()
            }

            player.onPaused = Runnable {
                AppLogger.d(TAG, "Desktop player is paused")
                updateState(playing = false, buffering = false)
                stopProgressUpdate()
            }

            player.onStopped = Runnable {
                AppLogger.d(TAG, "Desktop player is stopped")
                updateState(playing = false, buffering = false)
                stopProgressUpdate()
            }

            player.onEndOfMedia = Runnable {
                AppLogger.d(TAG, "Desktop player reached end of media")
                playNext()
            }

            player.onReady = Runnable {
                val duration = player.totalDuration.toMillis().toLong()
                AppLogger.d(TAG, "Desktop player ready. Duration: $duration ms")
                updateState(duration = duration, buffering = false)
                if (positionMs > 0) {
                    player.seek(Duration.millis(positionMs.toDouble()))
                }
                player.rate = _playerState.value.speed.toDouble()
            }

            player.onError = Runnable {
                AppLogger.e(TAG, "JavaFX MediaPlayer error: ${player.error?.message}")
                updateState(playing = false, buffering = false)
            }

            _playerState.value = _playerState.value.copy(currentEpisode = episode)

            if (autoPlay) {
                player.play()
            }
        } catch (e: Throwable) {
            AppLogger.e(TAG, "Error preparing media in Desktop", e)
            updateState(buffering = false)
        }
    }

    actual suspend fun play(episode: Episode) {
        internalPrepare(episode, 0, true)
    }

    actual fun prepare(episode: Episode, positionMs: Long) {
        internalPrepare(episode, positionMs, false)
    }

    actual fun pause() {
        setSpeed(1f)
        mediaPlayer?.pause()
    }

    actual fun resume() {
        val player = mediaPlayer
        if (player == null) {
            val currentEpisode = _playerState.value.currentEpisode
            if (currentEpisode != null) {
                internalPrepare(currentEpisode, _playerState.value.position, true)
            }
            return
        }

        updateState(buffering = true)
        player.play()
    }

    actual fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.dispose()
        mediaPlayer = null
        stopProgressUpdate()
        updateState(playing = false, buffering = false)
    }

    actual fun seekTo(positionMs: Long) {
        mediaPlayer?.seek(Duration.millis(positionMs.toDouble()))
    }

    actual fun setSpeed(speed: Float) {
        if (!_playerState.value.isPlaying) return
        _playerState.value = _playerState.value.copy(speed = speed)
        mediaPlayer?.rate = speed.toDouble()
    }

    actual fun skipForward(seconds: Int) {
        mediaPlayer?.let {
            val newTime = it.currentTime.add(Duration.seconds(seconds.toDouble()))
            it.seek(newTime)
        }
    }

    actual fun skipBackward(seconds: Int) {
        mediaPlayer?.let {
            val newTime = it.currentTime.subtract(Duration.seconds(seconds.toDouble()))
            it.seek(newTime)
        }
    }

    actual fun setSleepTimer(millis: Long?, selectedMinutes: Int?) {
        _playerState.value = _playerState.value.copy(
            sleepTimerMillis = millis,
            selectedSleepTimerMinutes = selectedMinutes
        )
    }

    actual fun setQueue(episodes: List<Episode>) {
        _playerState.value = _playerState.value.copy(queue = episodes)
    }

    actual fun playNext() {
        val state = _playerState.value
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentEpisode?.id }
        if (currentIndex != -1 && currentIndex < state.queue.size - 1) {
            val nextEpisode = state.queue[currentIndex + 1]
            internalPrepare(nextEpisode, 0, true)
        } else {
            stop()
        }
    }

    actual fun playPrevious() {
        val state = _playerState.value
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentEpisode?.id }
        if (currentIndex > 0) {
            val prevEpisode = state.queue[currentIndex - 1]
            internalPrepare(prevEpisode, 0, true)
        } else {
            seekTo(0)
        }
    }

    actual fun release() {
        stop()
        scope.cancel()
    }

    private fun updateState(
        playing: Boolean = _playerState.value.isPlaying,
        duration: Long = _playerState.value.duration ?: 0,
        buffering: Boolean = _playerState.value.isBuffering
    ) {
        _playerState.value = _playerState.value.copy(
            isPlaying = playing,
            duration = if (duration > 0) duration else _playerState.value.duration,
            isBuffering = buffering
        )
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.status == MediaPlayer.Status.PLAYING && player.rate != _playerState.value.speed.toDouble()) {
                        player.rate = _playerState.value.speed.toDouble()
                    }

                    _playerState.value = _playerState.value.copy(
                        position = player.currentTime.toMillis().toLong(),
                        duration = player.totalDuration.toMillis().toLong().takeIf { d -> d >= 0 }
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
