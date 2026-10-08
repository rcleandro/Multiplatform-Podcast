package br.com.carvalho.podcast.core.observability

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics

/**
 * True once the default Firebase app exists. Firebase starts in the background (`initKoin`), so the first calls
 * may arrive before it; they are dropped instead of crashing or being cached as "unavailable" for good.
 */
internal expect fun isFirebaseConfigured(): Boolean

class FirebaseAnalytics : Analytics {
    override fun logEvent(event: AnalyticsEvent) {
        if (!isFirebaseConfigured()) return
        val values = event.params.filterValues { it != null }.mapValues { it.value!! }
        Firebase.analytics.logEvent(event.name, values.ifEmpty { null })
    }
}

class FirebaseCrashReporter : CrashReporter {
    override fun log(message: String) {
        if (isFirebaseConfigured()) Firebase.crashlytics.log(message)
    }

    override fun recordException(throwable: Throwable) {
        if (isFirebaseConfigured()) Firebase.crashlytics.recordException(throwable)
    }
}
