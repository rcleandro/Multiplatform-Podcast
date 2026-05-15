package br.com.carvalho.podcast.domain.player

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.core.util.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFoundation.*
import platform.Foundation.*
import platform.CoreMedia.*
import kotlinx.cinterop.ExperimentalForeignApi

private const val TAG = "AudioPlayer"

@OptIn(ExperimentalForeignApi::class)
actual class AudioPlayer actual constructor() {
    private var avPlayer: AVPlayer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    actual val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    actual val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    init {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = null,
            usingBlock = {
                playNext()
            }
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
            scope.launch { play(nextEpisode) }
        } else {
            stop()
        }
    }

    actual fun playPrevious() {
        val state = _playerState.value
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentEpisode?.id }
        if (currentIndex > 0) {
            val prevEpisode = state.queue[currentIndex - 1]
            scope.launch { play(prevEpisode) }
        } else {
            seekTo(0)
        }
    }

    actual suspend fun play(episode: Episode) {
        AppLogger.i(TAG, "Playing episode in iOS: ${episode.title}")
        val url = if (episode.localPath != null) {
            NSURL.fileURLWithPath(episode.localPath)
        } else {
            NSURL.URLWithString(episode.audioUrl)
        } ?: run {
            AppLogger.e(TAG, "Invalid audio URL: ${episode.audioUrl}")
            return
        }
        val playerItem = AVPlayerItem.playerItemWithURL(url)

        if (avPlayer == null) {
            avPlayer = AVPlayer.playerWithPlayerItem(playerItem)
        } else {
            avPlayer?.replaceCurrentItemWithPlayerItem(playerItem)
        }

        avPlayer?.play()
        _playerState.value = _playerState.value.copy(
            currentEpisode = episode,
            isPlaying = true
        )
        startProgressUpdate()
    }

    actual fun prepare(episode: Episode, positionMs: Long) {
        AppLogger.d(TAG, "Preparing episode in iOS: ${episode.title}")
        val url = if (episode.localPath != null) {
            NSURL.fileURLWithPath(episode.localPath)
        } else {
            NSURL.URLWithString(episode.audioUrl)
        } ?: return
        val playerItem = AVPlayerItem.playerItemWithURL(url)


        if (avPlayer == null) {
            avPlayer = AVPlayer.playerWithPlayerItem(playerItem)
        } else {
            avPlayer?.replaceCurrentItemWithPlayerItem(playerItem)
        }

        avPlayer?.seekToTime(CMTimeMake(positionMs, 1000))

        _playerState.value = _playerState.value.copy(
            currentEpisode = episode,
            position = positionMs,
            duration = episode.duration * 1000
        )
    }

    actual fun pause() {
        setSpeed(1.0f)
        avPlayer?.pause()
        _playerState.value = _playerState.value.copy(isPlaying = false)
        stopProgressUpdate()
    }

    actual fun resume() {
        avPlayer?.setRate(_playerState.value.speed)
        _playerState.value = _playerState.value.copy(isPlaying = true)
        startProgressUpdate()
    }

    actual fun stop() {
        avPlayer?.pause()
        avPlayer?.replaceCurrentItemWithPlayerItem(null)
        _playerState.value = _playerState.value.copy(isPlaying = false)
        stopProgressUpdate()
    }

    actual fun seekTo(positionMs: Long) {
        val time = CMTimeMake(positionMs, 1000)
        avPlayer?.seekToTime(time)
    }

    actual fun setSpeed(speed: Float) {
        if (!_playerState.value.isPlaying) return
        _playerState.value = _playerState.value.copy(speed = speed)
        avPlayer?.rate = speed
    }

    actual fun skipForward(seconds: Int) {
        val currentTime = avPlayer?.currentTime() ?: return
        val newTime = CMTimeAdd(currentTime, CMTimeMakeWithSeconds(seconds.toDouble(), 1))
        avPlayer?.seekToTime(newTime)
    }

    actual fun skipBackward(seconds: Int) {
        val currentTime = avPlayer?.currentTime() ?: return
        val newTime = CMTimeSubtract(currentTime, CMTimeMakeWithSeconds(seconds.toDouble(), 1))
        avPlayer?.seekToTime(newTime)
    }

    actual fun setSleepTimer(millis: Long?, selectedMinutes: Int?) {
        _playerState.value = _playerState.value.copy(
            sleepTimerMillis = millis,
            selectedSleepTimerMinutes = selectedMinutes
        )
    }

    actual fun release() {
        stopProgressUpdate()
        avPlayer?.pause()
        avPlayer = null
        scope.cancel()
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val currentItem = avPlayer?.currentItem
                if (currentItem != null) {
                    val duration = CMTimeGetSeconds(currentItem.duration)
                    val currentTime = CMTimeGetSeconds(avPlayer?.currentTime() ?: CMTimeMake(0, 1))

                    if (!duration.isNaN()) {
                        _playerState.value = _playerState.value.copy(
                            position = (currentTime * 1000).toLong(),
                            duration = (duration * 1000).toLong()
                        )
                    }
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
