package br.com.carvalho.podcast.domain.model

/**
 * How much was heard (19.5): real time spent listening, and the time speed and skipped silences saved (the episode
 * moved further than the clock; negative when it played slower than normal).
 */
data class ListeningStats(
    /** The last [WEEKS] weeks, oldest first; the last one is this week. Weeks start on Monday. */
    val weeks: List<WeekListening> = emptyList(),
    /** Every podcast heard, the most heard first. A podcast removed from the library is left out. */
    val podcasts: List<PodcastListening> = emptyList(),
    val totalListenedMs: Long = 0,
    val totalSavedMs: Long = 0,
) {
    val thisWeekListenedMs: Long get() = weeks.lastOrNull()?.listenedMs ?: 0

    companion object {
        const val WEEKS = 8
    }
}

/** A week by its Monday, as days since 1970-01-01 in the device's time zone. */
data class WeekListening(val startEpochDay: Long, val listenedMs: Long)

data class PodcastListening(val podcast: Podcast, val listenedMs: Long)
