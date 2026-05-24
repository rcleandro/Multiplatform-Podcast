package br.com.carvalho.podcast.core.analytics

actual object Analytics {
    actual fun logEvent(name: String, params: Map<String, Any?>) {}

    actual fun setUserId(userId: String?) {}

    actual fun setUserProperty(name: String, value: String?) {}
}
