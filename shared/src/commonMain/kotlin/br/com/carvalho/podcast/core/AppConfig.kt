package br.com.carvalho.podcast.core

import br.com.carvalho.podcast.core.config.RemoteConfig

object AppConfig {
    private const val DEFAULT_SKIP_FORWARD_SECONDS = 30
    private const val DEFAULT_SKIP_BACKWARD_SECONDS = 10

    val SKIP_FORWARD_SECONDS: Int
        get() = RemoteConfig.getLong("skip_forward_seconds").toInt().takeIf { it > 0 } ?: DEFAULT_SKIP_FORWARD_SECONDS
        
    val SKIP_BACKWARD_SECONDS: Int
        get() = RemoteConfig.getLong("skip_backward_seconds").toInt().takeIf { it > 0 } ?: DEFAULT_SKIP_BACKWARD_SECONDS
        
    const val PLAYBACK_SAVE_DEBOUNCE_MS = 2000L
    const val PLAYBACK_FINISHED_THRESHOLD = 0.95f
    const val SLEEP_TIMER_TICK_MS = 1000L
    const val SEARCH_DEBOUNCE_MS = 300L
    const val DOWNLOAD_BUFFER_SIZE = 8192
    const val MILLIS_PER_SECOND = 1000L
    
    val PLAYBACK_SPEEDS = listOf(0.5f, 0.8f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f)
}
