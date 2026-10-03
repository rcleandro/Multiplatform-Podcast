package br.com.carvalho.podcast.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

private const val TAG = "PlayerViewModel"
class PlayerViewModel(
    private val audioPlayer: AudioPlayer,
    private val playerRepository: PlayerRepository,
    private val podcastRepository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = audioPlayer.playerState
    private var sleepTimerJob: Job? = null

    private var restored = false

    init {
        viewModelScope.launch(dispatchers.io) {
            audioPlayer.isReady
                .filter { it }
                .collect {
                    if (restored) {
                        AppLogger.d(TAG, "isReady re-emitted but already restored, ignoring")
                        return@collect
                    }
                    restored = true

                    AppLogger.d(TAG, "Restoring playback state...")
                    playerRepository.getSavedPlaybackState()?.let { saved ->
                        saved.episodeId?.let { id ->
                            val episode = podcastRepository.getEpisodeById(id)
                            if (episode != null) {
                                val resolvedEpisode = episode.copy(localPath = episodeDownloader.getLocalPath(episode.id))
                                AppLogger.i(TAG, "Restoring episode: ${resolvedEpisode.title} at ${saved.position}ms")
                                audioPlayer.setQueue(saved.queue)
                                audioPlayer.prepare(resolvedEpisode, saved.position)
                            }
                        }
                        audioPlayer.setSpeed(saved.speed)
                    }
                }
        }

        // Saves whenever playback moved one interval past the last save, or the episode changed. A debounce never
        // fired here (the position changes every 500 ms), and a timer would tick even with nothing to save.
        playerState
            .filter { it.isPlaying }
            .distinctUntilChanged { saved, now ->
                now.currentEpisode?.id == saved.currentEpisode?.id &&
                    abs(now.position - saved.position) < AppConfig.PLAYBACK_SAVE_INTERVAL_MS
            }
            .onEach { saveState(it) }
            .launchIn(viewModelScope)

        playerState
            .filter { !it.isPlaying && it.currentEpisode != null }
            .distinctUntilChanged { old, new -> old.position == new.position }
            .onEach { saveState(it) }
            .launchIn(viewModelScope)
    }

    private suspend fun saveState(state: PlayerState) = withContext(dispatchers.io) {
        val episode = state.currentEpisode ?: return@withContext
        playerRepository.savePlaybackState(
            episodeId = episode.id,
            position = state.position,
            speed = state.speed,
            queue = state.queue
        )
        val duration = state.duration
        if (duration != null && duration > 0 && state.position > duration * AppConfig.PLAYBACK_FINISHED_THRESHOLD) {
            podcastRepository.markEpisodeAsPlayed(episode.id)
        } else if (state.position > 0) {
            podcastRepository.updateEpisodeProgress(episode.id, state.position)
        }
    }

    fun onIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.Play -> play(intent.episode)
            PlayerIntent.PlayPause -> if (playerState.value.isPlaying) pause() else resume()
            is PlayerIntent.SeekTo -> seekTo(intent.positionMs)
            PlayerIntent.SkipForward -> skipForward()
            PlayerIntent.SkipBackward -> skipBackward()
            PlayerIntent.Next -> playNext()
            PlayerIntent.Previous -> playPrevious()
            is PlayerIntent.SetSpeed -> setSpeed(intent.speed)
            is PlayerIntent.SetSleepTimer -> setSleepTimer(intent.minutes)
        }
    }

    private fun play(episode: Episode) = viewModelScope.launch(dispatchers.io) {
        analytics.logEvent("play_episode", mapOf(
            "episode_id" to episode.id,
            "episode_title" to episode.title,
            "podcast_title" to episode.podcastTitle
        ))
        val resolvedEpisode = episode.copy(localPath = episodeDownloader.getLocalPath(episode.id))
        audioPlayer.play(resolvedEpisode)
    }

    private fun pause() {
        analytics.logEvent("pause_episode")
        audioPlayer.pause()
    }

    private fun resume() {
        analytics.logEvent("resume_episode")
        audioPlayer.resume()
    }

    private fun seekTo(positionMs: Long) {
        analytics.logEvent("seek_episode", mapOf("position_ms" to positionMs))
        audioPlayer.seekTo(positionMs)
    }

    private fun skipForward() {
        analytics.logEvent("skip_forward")
        audioPlayer.skipForward(seconds = AppConfig.SKIP_FORWARD_SECONDS)
    }

    private fun skipBackward() {
        analytics.logEvent("skip_backward")
        audioPlayer.skipBackward(seconds = AppConfig.SKIP_BACKWARD_SECONDS)
    }

    private fun setSpeed(speed: Float) {
        analytics.logEvent("set_speed", mapOf("speed" to speed))
        audioPlayer.setSpeed(speed)
    }

    private fun playNext() {
        analytics.logEvent("play_next")
        audioPlayer.playNext()
    }

    private fun playPrevious() {
        analytics.logEvent("play_previous")
        audioPlayer.playPrevious()
    }

    private fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null) {
            analytics.logEvent("cancel_sleep_timer")
            AppLogger.i(TAG, "Sleep timer cancelled")
            audioPlayer.setSleepTimer(null, null)
            return
        }

        analytics.logEvent("set_sleep_timer", mapOf("minutes" to minutes))
        AppLogger.i(TAG, "Setting sleep timer for $minutes minutes")
        val totalDuration = minutes.minutes
        val timeSource = TimeSource.Monotonic
        val mark = timeSource.markNow()

        sleepTimerJob = viewModelScope.launch(dispatchers.io) {
            var remaining = totalDuration
            while (remaining.isPositive()) {
                audioPlayer.setSleepTimer(remaining.inWholeMilliseconds, minutes)
                delay(AppConfig.SLEEP_TIMER_TICK_MS)
                remaining = totalDuration - mark.elapsedNow()
            }
            AppLogger.i(TAG, "Sleep timer finished. Pausing playback.")
            audioPlayer.setSleepTimer(null, null)
            pause()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        audioPlayer.release()
    }
}

sealed interface PlayerIntent {
    data class Play(val episode: Episode) : PlayerIntent
    data object PlayPause : PlayerIntent
    data class SeekTo(val positionMs: Long) : PlayerIntent
    data object SkipForward : PlayerIntent
    data object SkipBackward : PlayerIntent
    data object Next : PlayerIntent
    data object Previous : PlayerIntent
    data class SetSpeed(val speed: Float) : PlayerIntent
    /** `null` cancels the timer. */
    data class SetSleepTimer(val minutes: Int?) : PlayerIntent
}
