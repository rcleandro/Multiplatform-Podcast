package br.com.carvalho.podcast.domain.model

import br.com.carvalho.podcast.domain.player.SleepTimer

data class PlayerState(
    val currentEpisode: Episode? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long? = null,
    val speed: Float = 1f,
    val isBuffering: Boolean = false,
    val queue: List<Episode> = emptyList(),
    val sleepTimer: SleepTimer? = null,
    /** Time left on a [SleepTimer.Minutes] timer. */
    val sleepTimerMillis: Long? = null,
)
