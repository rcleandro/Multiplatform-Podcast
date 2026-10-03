package br.com.carvalho.podcast.core.util

import br.com.carvalho.podcast.core.observability.CrashReporter
import co.touchlab.kermit.Logger

/**
 * Static logging facade over Kermit. The crash reporter is plugged in at startup (`initKoin`), so this object
 * does not depend on Firebase and tests need no setup.
 */
object AppLogger {
    var crashReporter: CrashReporter? = null

    fun d(tag: String, message: String) {
        Logger.withTag(tag).d { message }
        crashReporter?.log("[DEBUG] $tag: $message")
    }

    fun i(tag: String, message: String) {
        Logger.withTag(tag).i { message }
        crashReporter?.log("[INFO] $tag: $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Logger.withTag(tag).e(throwable) { message }
        crashReporter?.log("[ERROR] $tag: $message")
        throwable?.let { crashReporter?.recordException(it) }
    }
}
