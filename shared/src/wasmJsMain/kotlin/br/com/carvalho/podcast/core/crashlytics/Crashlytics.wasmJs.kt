package br.com.carvalho.podcast.core.crashlytics

actual object Crashlytics {
    actual fun log(message: String) {
        // No-op
    }

    actual fun recordException(throwable: Throwable) {
        // No-op
    }

    actual fun setUserId(userId: String) {
        // No-op
    }

    actual fun setCustomKey(key: String, value: String) {
        // No-op
    }

    actual fun setCustomKey(key: String, value: Int) {
        // No-op
    }

    actual fun setCustomKey(key: String, value: Boolean) {
        // No-op
    }
}
