package br.com.carvalho.podcast.core.util

import co.touchlab.kermit.Logger
import br.com.carvalho.podcast.core.crashlytics.Crashlytics

object AppLogger {
    fun d(tag: String, message: String) {
        Logger.withTag(tag).d { message }
        runCatching { Crashlytics.log("[DEBUG] $tag: $message") }
    }

    fun i(tag: String, message: String) {
        Logger.withTag(tag).i { message }
        runCatching { Crashlytics.log("[INFO] $tag: $message") }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Logger.withTag(tag).e(throwable) { message }
        runCatching {
            Crashlytics.log("[ERROR] $tag: $message")
            throwable?.let { Crashlytics.recordException(it) }
        }
    }
}
