package br.com.carvalho.podcast.core.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(prefix, style, reset) => console.log(prefix, style, reset)")
external fun consoleLog(prefix: String, style: String, reset: String)

actual object AppLogger {
    actual fun d(tag: String, message: String) {
        log("DEBUG", tag, message, "color: #00bcd4;", null)
    }

    actual fun i(tag: String, message: String) {
        log("INFO", tag, message, "color: #4caf50;", null)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        log("ERROR", tag, message, "color: #f44336; font-weight: bold;", throwable)
    }

    private fun getCurrentTime(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hours = now.hour.toString().padStart(2, '0')
        val minutes = now.minute.toString().padStart(2, '0')
        val seconds = now.second.toString().padStart(2, '0')
        return "$hours:$minutes:$seconds"
    }

    private fun log(
        level: String,
        tag: String,
        message: String,
        style: String,
        throwable: Throwable?
    ) {
        val time = getCurrentTime()
        val logPrefix = "$time %c[$level] [$tag] %c$message"
        val resetStyle = "color: inherit;"

        consoleLog(logPrefix, style, resetStyle)

        if (throwable != null) {
            println(throwable.stackTraceToString())
        }
    }
}
