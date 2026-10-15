package br.com.carvalho.podcast.domain.model

import br.com.carvalho.podcast.domain.player.SleepTimer

data class PlayerState(
    val currentEpisode: Episode? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long? = null,
    val speed: Float = 1f,
    val isBuffering: Boolean = false,
    /** What plays next, in order; never the current episode (18.6). */
    val queue: List<Episode> = emptyList(),
    val sleepTimer: SleepTimer? = null,
    /** Time left on a [SleepTimer.Minutes] timer. */
    val sleepTimerMillis: Long? = null,
    /** The jumps chosen in Settings, for every button that skips. */
    val skipIntervals: SkipIntervals = SkipIntervals(),
    /** The current episode's chapters (19.1), in start order; empty until found, or when it has none. */
    val chapters: List<Chapter> = emptyList(),
) {
    val currentChapter: Chapter? get() = chapters.at(position)
}
