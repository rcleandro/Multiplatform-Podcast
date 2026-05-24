package br.com.carvalho.podcast.core.analytics

expect object Analytics {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
    fun setUserId(userId: String?)
    fun setUserProperty(name: String, value: String?)
}
