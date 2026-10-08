package br.com.carvalho.podcast.core.observability

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics
import kotlinx.coroutines.flow.StateFlow

/**
 * True once the default Firebase app exists. Firebase starts in the background (`initKoin`), so the first calls
 * may arrive before it; they are dropped instead of crashing or being cached as "unavailable" for good.
 */
internal expect fun isFirebaseConfigured(): Boolean

/**
 * Applies the user's choice to Firebase's own collection, which also covers its automatic events and crashes. Both
 * start off (manifest and Info.plist) until this runs, so a user who turned telemetry off sends nothing on launch.
 */
suspend fun followTelemetryConsent(enabled: StateFlow<Boolean>) {
    enabled.collect { on ->
        Firebase.analytics.setAnalyticsCollectionEnabled(on)
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(on)
        if (!on) Firebase.crashlytics.deleteUnsentReports()
    }
}

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
