package br.com.carvalho.podcast.domain.player

/** When playback should stop by itself. */
sealed interface SleepTimer {
    data class Minutes(val minutes: Int) : SleepTimer

    /** Pauses when the current episode ends instead of moving to the next one. */
    data object EndOfEpisode : SleepTimer
}
