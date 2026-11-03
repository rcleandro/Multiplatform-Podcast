package br.com.carvalho.podcast.watch

import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlinx.coroutines.flow.StateFlow

/** Minutes "+5 min" adds to a running timer, as on the phone (18.15). */
private const val TIMER_EXTENSION_MINUTES = 5

/** A sleep timer choice: the shared string that names it and the timer, null for off. */
data class WatchTimerOption(val key: String, val timer: SleepTimer?)

/**
 * The watch's player (25.14c), as the Wear OS one (19.9e, 19.9h): play and pause, the jumps Settings chose, speed,
 * sleep timer and the queue, all on the app's one player, which the system's Now Playing also drives.
 */
class WatchPlayer(private val player: AudioPlayer) {
    val state: StateFlow<PlayerState> = player.playerState

    val speeds: List<Float> = AppConfig.PLAYBACK_SPEEDS

    @Suppress("MagicNumber") // the choices themselves, as on the phone
    val timers: List<WatchTimerOption> = listOf(
        WatchTimerOption("timer_disabled", null),
        WatchTimerOption("timer_5_min", SleepTimer.Minutes(5)),
        WatchTimerOption("timer_15_min", SleepTimer.Minutes(15)),
        WatchTimerOption("timer_30_min", SleepTimer.Minutes(30)),
        WatchTimerOption("timer_45_min", SleepTimer.Minutes(45)),
        WatchTimerOption("timer_60_min", SleepTimer.Minutes(60)),
        WatchTimerOption("timer_end_of_episode", SleepTimer.EndOfEpisode),
    )

    fun playPause() {
        if (player.playerState.value.isPlaying) player.pause() else player.resume()
    }

    fun skipBackward() = player.skipBackward(player.playerState.value.skipIntervals.backwardSeconds)

    fun skipForward() = player.skipForward(player.playerState.value.skipIntervals.forwardSeconds)

    fun setSpeed(speed: Float) = player.setSpeed(speed)

    fun setSleepTimer(timer: SleepTimer?) = player.setSleepTimer(timer)

    fun extendSleepTimer() = player.extendSleepTimer(TIMER_EXTENSION_MINUTES)

    /** Plays an episode of the queue now. */
    suspend fun play(episode: Episode) = PlayEpisodeUseCase(player)(episode)
}

/** How far the episode is, 0 to 1, for the ring around the screen; 0 while its length is unknown. */
fun PlayerState.progress(): Float {
    val total = duration ?: currentEpisode?.duration?.times(AppConfig.MILLIS_PER_SECOND)
    return if (total == null || total <= 0) 0f else (position.toFloat() / total).coerceIn(0f, 1f)
}
