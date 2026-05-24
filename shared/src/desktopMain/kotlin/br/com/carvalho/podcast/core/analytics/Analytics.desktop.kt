package br.com.carvalho.podcast.core.analytics

actual object Analytics {
    actual fun logEvent(name: String, params: Map<String, Any?>) {
        // No-op for Desktop
    }

    actual fun setUserId(userId: String?) {
        // No-op for Desktop
    }

    actual fun setUserProperty(name: String, value: String?) {
        // No-op for Desktop
    }
}
