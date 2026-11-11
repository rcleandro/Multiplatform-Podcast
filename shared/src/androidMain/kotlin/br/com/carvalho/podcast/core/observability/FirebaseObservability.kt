package br.com.carvalho.podcast.core.observability

import com.google.firebase.FirebaseApp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics

/**
 * True once the default Firebase app exists. Firebase starts in the background (`initKoin`), so the first calls
 * may arrive before it; they are dropped instead of crashing or being cached as "unavailable" for good.
 */
private fun isFirebaseConfigured(): Boolean = runCatching { FirebaseApp.getInstance() }.isSuccess

/**
 * Firebase's collection on Android. Both start off in the manifest until the user's choice arrives
 * ([followTelemetryConsent]), so a user who turned telemetry off sends nothing on launch.
 */
object FirebaseCollection : TelemetryCollection {
    override fun setCollectionEnabled(enabled: Boolean) {
        Firebase.analytics.setAnalyticsCollectionEnabled(enabled)
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    override fun forgetCollected() {
        Firebase.crashlytics.deleteUnsentReports()
        Firebase.analytics.resetAnalyticsData()
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
