package br.com.carvalho.podcast.domain.player

import android.content.ComponentName
import android.content.Context
import br.com.carvalho.podcast.core.player.PodcastMediaService
import br.com.carvalho.podcast.core.util.AppContext
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import androidx.core.net.toUri
import br.com.carvalho.podcast.core.util.AppLogger

private const val TAG = "AudioPlayer"

class AndroidAudioPlayer : AudioPlayer {
    private val context = AppContext.context as Context
    private val sessionToken = SessionToken(
        context,
        ComponentName(context, PodcastMediaService::class.java)
    )
    private val controllerFuture by lazy {
        MediaController.Builder(context, sessionToken).buildAsync()
    }
    private var controller: MediaController? = null

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            AppLogger.d(TAG, "onIsPlayingChanged: isPlaying $isPlaying")
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            AppLogger.d(TAG, "onPlaybackStateChanged: $playbackState")

            if (playbackState == Player.STATE_ENDED) {
                playNext()
            }

            val isBuffering = playbackState == Player.STATE_BUFFERING

            _playerState.value = _playerState.value.copy(
                isBuffering = isBuffering,
                duration = controller?.duration?.let { if (it >= 0) it else null }
            )
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            AppLogger.d(TAG, "onPlaybackParametersChanged: $playbackParameters")

            _playerState.value = _playerState.value.copy(speed = playbackParameters.speed)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            AppLogger.d(TAG, "onPlayWhenReadyChanged: $playWhenReady reason=$reason")
        }
    }

    init {
        AppLogger.d(TAG, "AudioPlayer init, instance=${System.identityHashCode(this)}")

        scope.launch {
            try {
                AppLogger.d(TAG, "Connecting to MediaController...")
                controller = controllerFuture.await()
                AppLogger.d(TAG, "Controller ready, playbackState = ${controller?.playbackState}")

                controller?.addListener(playerListener)
                AppLogger.d(TAG, "MediaController connected")

                controller?.let { player ->
                    when (player.playbackState) {
                        Player.STATE_BUFFERING -> {
                            _playerState.value = _playerState.value.copy(isBuffering = true)
                        }
                        Player.STATE_READY -> {
                            _playerState.value = _playerState.value.copy(isBuffering = false)
                        }
                    }

                    if (player.isPlaying || player.playbackState != Player.STATE_IDLE) {
                        _playerState.value = _playerState.value.copy(
                            isPlaying = player.isPlaying,
                            speed = player.playbackParameters.speed,
                            duration = player.duration.takeIf { d -> d >= 0 },
                            position = player.currentPosition
                        )
                    } else {
                        player.playbackParameters = PlaybackParameters(_playerState.value.speed)
                    }

                    if (player.isPlaying) startProgressUpdate()
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to connect to MediaController", e)
            } finally {
                AppLogger.d(TAG, "isReady emitting true, previous = ${_isReady.value}")
                if (!_isReady.value) {
                    _isReady.value = true
                }
            }
        }
    }

    private suspend fun getController(): MediaController? {
        if (controller != null) return controller
        return try {
            controllerFuture.await().also {
                controller = it
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error awaiting MediaController", e)
            null
        }
    }

    override suspend fun play(episode: Episode) {
        val player = getController() ?: return
        withContext(Dispatchers.Main) {
            val playbackUri = if (episode.localPath != null) {
                android.net.Uri.fromFile(java.io.File(episode.localPath))
            } else {
                episode.audioUrl.toUri()
            }
            val mediaItem = MediaItem.Builder()
                .setMediaId(episode.id)
                .setUri(playbackUri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(episode.title)
                        .setArtworkUri(episode.imageUrl?.toUri())
                        .build()
                )
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _playerState.value = _playerState.value.copy(currentEpisode = episode)
        }
    }

    override fun prepare(episode: Episode, positionMs: Long) {
        _playerState.value = _playerState.value.copy(
            currentEpisode = episode,
            position = positionMs,
            duration = episode.duration * 1000,
            isPlaying = false
        )

        scope.launch {
            val player = getController() ?: run {
                AppLogger.e(TAG, "prepare: controller is null")
                return@launch
            }

            AppLogger.d(TAG, "prepare: playWhenReady = ${player.playWhenReady}, state = ${player.playbackState}")

            val playbackUri = if (episode.localPath != null) {
                android.net.Uri.fromFile(java.io.File(episode.localPath))
            } else {
                episode.audioUrl.toUri()
            }
            val mediaItem = MediaItem.Builder()
                .setMediaId(episode.id)
                .setUri(playbackUri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(episode.title)
                        .setArtworkUri(episode.imageUrl?.toUri())
                        .build()
                )
                .build()

            player.stop()
            player.playWhenReady = false
            player.setMediaItem(mediaItem, positionMs)
            player.prepare()

            AppLogger.d(TAG, "prepare: after prepare, playWhenReady = ${player.playWhenReady}")
        }
    }

    override fun pause() {
        _playerState.value = _playerState.value.copy(isPlaying = false)
        scope.launch {
            getController()?.pause()
        }
    }

    override fun resume() {
        val currentEpisode = _playerState.value.currentEpisode ?: return

        scope.launch {
            val player = getController() ?: return@launch
            if (player.playbackState == Player.STATE_IDLE || player.currentMediaItem == null) {
                prepare(currentEpisode, _playerState.value.position)
            }
            player.play()
        }
    }

    override fun stop() {
        _playerState.value = _playerState.value.copy(isPlaying = false, currentEpisode = null)
        scope.launch {
            getController()?.stop()
        }
    }

    override fun seekTo(positionMs: Long) {
        _playerState.value = _playerState.value.copy(position = positionMs)
        scope.launch {
            getController()?.seekTo(positionMs)
        }
    }

    override fun setSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(speed = speed)
        scope.launch {
            getController()?.let {
                it.playbackParameters = PlaybackParameters(speed)
            }
        }
    }

    override fun skipForward(seconds: Int) {
        scope.launch {
            getController()?.let {
                it.seekTo(it.currentPosition + seconds * 1000)
            }
        }
    }

    override fun skipBackward(seconds: Int) {
        scope.launch {
            getController()?.let {
                it.seekTo(it.currentPosition - seconds * 1000)
            }
        }
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
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
        scope.cancel()
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                controller?.let { player ->
                    _playerState.value = _playerState.value.copy(
                        position = player.currentPosition,
                        duration = controller?.duration?.let { if (it >= 0) it else null }
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

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
