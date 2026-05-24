package br.com.carvalho.podcast.core.crashlytics

expect object Crashlytics {
    fun log(message: String)
    fun recordException(throwable: Throwable)
    fun setUserId(userId: String)
    fun setCustomKey(key: String, value: String)
    fun setCustomKey(key: String, value: Int)
    fun setCustomKey(key: String, value: Boolean)
}
