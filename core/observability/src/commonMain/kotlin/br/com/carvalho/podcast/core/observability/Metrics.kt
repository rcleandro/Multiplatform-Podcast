package br.com.carvalho.podcast.core.observability

import br.com.carvalho.podcast.core.util.AppLogger
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Health measurements as one structured info line each (`metric=<name> ms=<n> key=value…`), readable in the console
 * and in crash reports' breadcrumbs. The names and their budgets are in `docs/metricas.md`. Fields carry counts and
 * outcomes only, never a URL or a title.
 */
object Metrics {
    private const val TAG = "Metrics"

    /** Taken the first time anything touches [Metrics]: `initKoin` does so, the earliest point common code sees. */
    private val processStart = TimeSource.Monotonic.markNow()
    private var appStartRecorded = false

    fun record(name: String, duration: Duration, vararg fields: Pair<String, Any>) {
        val extra = fields.joinToString("") { (key, value) -> " $key=$value" }
        AppLogger.i(TAG, "metric=$name ms=${duration.inWholeMilliseconds}$extra")
    }

    /** Marks the start; call it first thing at launch. */
    fun markProcessStart() {
        processStart
    }

    /** From launch to the library on screen, once per process. */
    fun recordAppStart(vararg fields: Pair<String, Any>) {
        if (appStartRecorded) return
        appStartRecorded = true
        record(APP_START, processStart.elapsedNow(), *fields)
    }

    const val APP_START = "app_start"
    const val TIME_TO_AUDIO = "time_to_audio"
    const val FEED_REFRESH = "feed_refresh"
    const val REFRESH_ALL = "refresh_all"
    const val DOWNLOAD = "download"
}
