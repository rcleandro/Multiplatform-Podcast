package br.com.carvalho.podcast.core.analytics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics

actual object Analytics {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private val firebaseAnalytics by lazy {
        try {
            Firebase.analytics
        } catch (_: Exception) {
            null
        }
    }
    actual fun logEvent(name: String, params: Map<String, Any?>) {
        val nonNullParams = params.filterValues { it != null }.mapValues { it.value!! }
        firebaseAnalytics?.logEvent(name, nonNullParams.ifEmpty { null })
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
