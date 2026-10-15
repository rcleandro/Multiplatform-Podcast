package br.com.carvalho.podcast.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.AnalyticsEvent
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Bookmark
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The player screen's commands; the playback rules and the session live in the app's AudioPlayer. */
@Suppress("TooManyFunctions") // one function per player intent
class PlayerViewModel(
    private val audioPlayer: AudioPlayer,
    private val playEpisode: PlayEpisodeUseCase,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = audioPlayer.playerState

    /** The bookmarks of the episode playing (19.3). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarks: StateFlow<List<Bookmark>> = playerState
        .map { it.currentEpisode?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> id?.let(playerRepository::bookmarks) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_SHARING_MS), emptyList())

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
            PlayerIntent.ExtendSleepTimer -> {
                analytics.logEvent(AnalyticsEvent.ExtendSleepTimer(SLEEP_TIMER_EXTENSION_MINUTES))
                audioPlayer.extendSleepTimer(SLEEP_TIMER_EXTENSION_MINUTES)
            }
            is PlayerIntent.AddBookmark -> addBookmark(intent.note)
            is PlayerIntent.DeleteBookmark -> viewModelScope.launch(dispatchers.io) {
                analytics.logEvent(AnalyticsEvent.DeleteBookmark)
                playerRepository.deleteBookmark(intent.id)
            }
        }
    }

    /** Where the episode is now, with the note given. */
    private fun addBookmark(note: String) {
        val state = playerState.value
        val episode = state.currentEpisode ?: return
        analytics.logEvent(AnalyticsEvent.AddBookmark(hasNote = note.isNotBlank()))
        viewModelScope.launch(dispatchers.io) { playerRepository.addBookmark(episode.id, state.position, note) }
    }

    private fun play(episode: Episode) = viewModelScope.launch(dispatchers.io) {
        analytics.logEvent(AnalyticsEvent.PlayInPlayer(episode.id))
        // Picked from the queue: keep the queue as it is.
        playEpisode(episode)
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
        audioPlayer.skipForward(seconds = playerState.value.skipIntervals.forwardSeconds)
    }

    private fun skipBackward() {
        analytics.logEvent(AnalyticsEvent.SkipBackward)
        audioPlayer.skipBackward(seconds = playerState.value.skipIntervals.backwardSeconds)
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

    /** [SLEEP_TIMER_EXTENSION_MINUTES] more on a running timer (18.15). */
    data object ExtendSleepTimer : PlayerIntent

    /** Marks where the episode is now (19.3); [note] may be blank. */
    data class AddBookmark(val note: String) : PlayerIntent
    data class DeleteBookmark(val id: Long) : PlayerIntent
}

/** How long the bookmarks keep being watched after the player screen goes away, e.g. while it rotates. */
private const val STOP_SHARING_MS = 5_000L

/** What one tap on "+5 min" adds to the sleep timer. */
const val SLEEP_TIMER_EXTENSION_MINUTES = 5
