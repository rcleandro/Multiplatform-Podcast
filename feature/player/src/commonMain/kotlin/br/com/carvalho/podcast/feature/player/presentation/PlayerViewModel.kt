package br.com.carvalho.podcast.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.AnalyticsEvent
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The player screen's commands; the playback rules and the session live in the app's AudioPlayer. */
@Suppress("TooManyFunctions") // one function per player intent
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
        analytics.logEvent(AnalyticsEvent.PlayInPlayer(episode.id))
        // Picked from the queue: keep the queue as it is.
        playEpisode(episode, queue = playerState.value.queue)
    }

    private fun pause() {
        analytics.logEvent(AnalyticsEvent.Pause)
        audioPlayer.pause()
    }

    private fun resume() {
        analytics.logEvent(AnalyticsEvent.Resume)
        audioPlayer.resume()
    }

    private fun seekTo(positionMs: Long) {
        analytics.logEvent(AnalyticsEvent.Seek(positionMs))
        audioPlayer.seekTo(positionMs)
    }

    private fun skipForward() {
        analytics.logEvent(AnalyticsEvent.SkipForward)
        audioPlayer.skipForward(seconds = AppConfig.SKIP_FORWARD_SECONDS)
    }

    private fun skipBackward() {
        analytics.logEvent(AnalyticsEvent.SkipBackward)
        audioPlayer.skipBackward(seconds = AppConfig.SKIP_BACKWARD_SECONDS)
    }

    private fun setSpeed(speed: Float) {
        analytics.logEvent(AnalyticsEvent.SetSpeed(speed))
        audioPlayer.setSpeed(speed)
    }

    private fun playNext() {
        analytics.logEvent(AnalyticsEvent.PlayNext)
        audioPlayer.playNext()
    }

    private fun playPrevious() {
        analytics.logEvent(AnalyticsEvent.PlayPrevious)
        audioPlayer.playPrevious()
    }

    private fun setSleepTimer(timer: SleepTimer?) {
        when (timer) {
            null -> analytics.logEvent(AnalyticsEvent.CancelSleepTimer)
            is SleepTimer.Minutes -> analytics.logEvent(AnalyticsEvent.SetSleepTimer(timer.minutes))
            SleepTimer.EndOfEpisode -> analytics.logEvent(AnalyticsEvent.SetSleepTimer(null))
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
