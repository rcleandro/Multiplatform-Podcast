package br.com.carvalho.podcast.core

import br.com.carvalho.podcast.core.config.RemoteConfig

object AppConfig {
    private const val DEFAULT_SKIP_FORWARD_SECONDS = 30
    private const val SKIP_FORWARD_SECONDS_KEY = "skip_forward_seconds"
    val SKIP_FORWARD_SECONDS: Int
        get() = RemoteConfig.getLong(SKIP_FORWARD_SECONDS_KEY).toInt()
            .takeIf { it > 0 } ?: DEFAULT_SKIP_FORWARD_SECONDS

    private const val DEFAULT_SKIP_BACKWARD_SECONDS = 10
    private const val SKIP_BACKWARD_SECONDS_KEY = "skip_backward_seconds"
    val SKIP_BACKWARD_SECONDS: Int
        get() = RemoteConfig.getLong(SKIP_BACKWARD_SECONDS_KEY).toInt()
            .takeIf { it > 0 } ?: DEFAULT_SKIP_BACKWARD_SECONDS

    private const val DEFAULT_PLAYBACK_SAVE_DEBOUNCE_MS = 2000L
    private const val PLAYBACK_SAVE_DEBOUNCE_MS_KEY = "playback_save_debounce_ms"
    val PLAYBACK_SAVE_DEBOUNCE_MS: Long
        get() = RemoteConfig.getLong(PLAYBACK_SAVE_DEBOUNCE_MS_KEY)
            .takeIf { it > 0 } ?: DEFAULT_PLAYBACK_SAVE_DEBOUNCE_MS

    private const val DEFAULT_PLAYBACK_FINISHED_THRESHOLD = 0.95f
    private const val PLAYBACK_FINISHED_THRESHOLD_KEY = "playback_finished_threshold"
    val PLAYBACK_FINISHED_THRESHOLD: Float
        get() = RemoteConfig.getDouble(PLAYBACK_FINISHED_THRESHOLD_KEY).toFloat()
            .takeIf { it > 0f } ?: DEFAULT_PLAYBACK_FINISHED_THRESHOLD

    private const val DEFAULT_SLEEP_TIMER_TICK_MS = 1000L
    private const val SLEEP_TIMER_TICK_MS_KEY = "sleep_timer_tick_ms"
    val SLEEP_TIMER_TICK_MS: Long
        get() = RemoteConfig.getLong(SLEEP_TIMER_TICK_MS_KEY)
            .takeIf { it > 0 } ?: DEFAULT_SLEEP_TIMER_TICK_MS

    private const val DEFAULT_SEARCH_DEBOUNCE_MS = 300L
    private const val SEARCH_DEBOUNCE_MS_KEY = "search_debounce_ms"
    val SEARCH_DEBOUNCE_MS: Long
        get() = RemoteConfig.getLong(SEARCH_DEBOUNCE_MS_KEY)
            .takeIf { it > 0 } ?: DEFAULT_SEARCH_DEBOUNCE_MS

    private const val DEFAULT_DOWNLOAD_BUFFER_SIZE = 8192
    private const val DOWNLOAD_BUFFER_SIZE_KEY = "download_buffer_size"
    val DOWNLOAD_BUFFER_SIZE: Int
        get() = RemoteConfig.getLong(DOWNLOAD_BUFFER_SIZE_KEY).toInt()
            .takeIf { it > 0 } ?: DEFAULT_DOWNLOAD_BUFFER_SIZE

    private const val DEFAULT_MILLIS_PER_SECOND = 1000L
    private const val MILLIS_PER_SECOND_KEY = "millis_per_second"
    val MILLIS_PER_SECOND: Long
        get() = RemoteConfig.getLong(MILLIS_PER_SECOND_KEY)
            .takeIf { it > 0 } ?: DEFAULT_MILLIS_PER_SECOND

    val DEFAULT_PLAYBACK_SPEEDS = listOf(0.5f, 0.8f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f)
    val PLAYBACK_SPEEDS: List<Float>
        get() = RemoteConfig.getString("playback_speeds")
            .split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
            .takeIf { it.isNotEmpty() }
            ?: DEFAULT_PLAYBACK_SPEEDS
}
