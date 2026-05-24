package br.com.carvalho.podcast.core.crashlytics

actual object Crashlytics {
    actual fun log(message: String) {}
    actual fun recordException(throwable: Throwable) {}
    actual fun setUserId(userId: String) {}
    actual fun setCustomKey(key: String, value: String) {}
    actual fun setCustomKey(key: String, value: Int) {}
    actual fun setCustomKey(key: String, value: Boolean) {}
}
