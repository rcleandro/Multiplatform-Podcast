package br.com.carvalho.podcast.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The player screen's commands; the playback rules and the session live in the app's AudioPlayer. */
class PlayerViewModel(
    private val audioPlayer: AudioPlayer,
    private val playEpisode: PlayEpisodeUseCase,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = audioPlayer.playerState

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
            is PlayerIntent.SetSleepTimer -> setSleepTimer(intent.timer)
        }
    }

    private fun play(episode: Episode) = viewModelScope.launch(dispatchers.io) {
        analytics.logEvent("play_episode", mapOf(
            "episode_id" to episode.id,
            "episode_title" to episode.title,
            "podcast_title" to episode.podcastTitle
        ))
        // Picked from the queue: keep the queue as it is.
        playEpisode(episode, queue = playerState.value.queue)
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

    private fun setSleepTimer(timer: SleepTimer?) {
        when (timer) {
            null -> analytics.logEvent("cancel_sleep_timer")
            is SleepTimer.Minutes -> analytics.logEvent("set_sleep_timer", mapOf("minutes" to timer.minutes))
            SleepTimer.EndOfEpisode -> analytics.logEvent("set_sleep_timer", mapOf("minutes" to "end_of_episode"))
        }
        audioPlayer.setSleepTimer(timer)
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
    data class SetSleepTimer(val timer: SleepTimer?) : PlayerIntent
}
