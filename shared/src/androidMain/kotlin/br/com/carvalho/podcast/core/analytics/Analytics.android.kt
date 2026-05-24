package br.com.carvalho.podcast.core.analytics

import br.com.carvalho.podcast.core.util.AppLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics

private const val TAG = "Analytics"

actual object Analytics {
    private val firebaseAnalytics by lazy {
        try {
            Firebase.analytics
        } catch (e: Exception) {
            AppLogger.e(TAG, "Firebase Analytics not available (expected in unit tests)", e)
            null
        }
    }

    actual fun logEvent(name: String, params: Map<String, Any?>) {
        val nonNullParams = params.filterValues { it != null }.mapValues { it.value!! }
        firebaseAnalytics?.logEvent(name, nonNullParams)
    }

    actual fun setUserId(userId: String?) {
        firebaseAnalytics?.setUserId(userId)
    }

    actual fun setUserProperty(name: String, value: String?) {
        value?.let {
            firebaseAnalytics?.setUserProperty(name, it)
        }
    }
}
