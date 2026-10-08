package br.com.carvalho.podcast.core.util

import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.observability.redactUrls
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

/**
 * Static logging facade over Kermit. The crash reporter is plugged in at startup (`initKoin`), so this object
 * does not depend on Firebase and tests need no setup. URLs are cut to their host on the way in (feed tokens).
 */
object AppLogger {
    var crashReporter: CrashReporter? = null

    /** Debug builds log everything, HTTP included; release builds start at info. Set before `startKoin`. */
    var isDebugBuild: Boolean = false
        set(value) {
            field = value
            Logger.setMinSeverity(if (value) Severity.Verbose else Severity.Info)
        }

    init {
        Logger.setMinSeverity(Severity.Info)
    }

    /** Console only: debug lines (HTTP among them) would flood the crash reporter's breadcrumbs. */
    fun d(tag: String, message: String) {
        Logger.withTag(tag).d { redactUrls(message) }
    }

    fun i(tag: String, message: String) {
        val safe = redactUrls(message)
        Logger.withTag(tag).i { safe }
        crashReporter?.log("[INFO] $tag: $safe")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val safe = redactUrls(message)
        val safeThrowable = throwable?.withoutUrls()
        Logger.withTag(tag).e(safeThrowable) { safe }
        crashReporter?.log("[ERROR] $tag: $safe")
        safeThrowable?.let { crashReporter?.recordException(it) }
    }

    // ponytail: the stand-in keeps the class name and the redacted message, not the original stack trace (common
    // code cannot copy it); add expect/actual copying `stackTrace` on JVM if Crashlytics grouping suffers.
    private fun Throwable.withoutUrls(): Throwable {
        val chain = generateSequence(this) { it.cause }.toList()
        if (chain.none { redactUrls(it.message.orEmpty()) != it.message.orEmpty() }) return this
        return RedactedException(
            chain.joinToString(" <- ") { "${it::class.simpleName}: ${redactUrls(it.message.orEmpty())}" }
        )
    }
}

/** Replaces an exception whose message (or a cause's) carried a URL. */
class RedactedException(message: String) : Exception(message)
