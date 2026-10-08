package br.com.carvalho.podcast.core.observability

import br.com.carvalho.podcast.core.util.AppLogger

/** Product analytics events. The Firebase implementation lives in `firebaseMain` (Android and iOS). */
interface Analytics {
    fun logEvent(event: AnalyticsEvent)
}

/** Crash and error reporting; receives the logs [AppLogger] forwards. */
interface CrashReporter {
    fun log(message: String)
    fun recordException(throwable: Throwable)
}

/** Desktop and Web have no analytics backend: events only go to the log. */
class LogAnalytics : Analytics {
    override fun logEvent(event: AnalyticsEvent) {
        AppLogger.d(TAG, if (event.params.isEmpty()) event.name else "${event.name} ${event.params}")
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
