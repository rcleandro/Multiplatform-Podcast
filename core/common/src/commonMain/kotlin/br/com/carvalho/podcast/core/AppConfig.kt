package br.com.carvalho.podcast.core

/** Tuning values of the app. They used to come from Firebase Remote Config; these were already the defaults. */
object AppConfig {
    const val SKIP_FORWARD_SECONDS = 30
    const val SKIP_BACKWARD_SECONDS = 10

    /** How often the progress is saved while playing: at most this much is lost if the process dies. */
    const val PLAYBACK_SAVE_INTERVAL_MS = 5000L
    const val PLAYBACK_FINISHED_THRESHOLD = 0.95f
    const val SLEEP_TIMER_TICK_MS = 1000L
    const val SEARCH_DEBOUNCE_MS = 300L
    const val DOWNLOAD_BUFFER_SIZE = 8192
    const val MILLIS_PER_SECOND = 1000L

    /**
     * The most the app reads of a feed or of an imported file (OPML, backup, the sync folder): 30 MB, far above any
     * real one and well within a watch's memory (22.3, T2 and T12 of ADR 0014).
     */
    const val MAX_TEXT_BYTES = 30L * 1024 * 1024

    @Suppress("MagicNumber") // the speeds are the values themselves
    val PLAYBACK_SPEEDS = listOf(0.5f, 0.8f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f)
}
