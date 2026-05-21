package br.com.carvalho.podcast.core.util

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

    private fun log(
        level: String,
        tag: String,
        message: String,
        style: String,
        throwable: Throwable?
    ) {
        val logPrefix = "%c[$level] [$tag] %c$message"
        val resetStyle = "color: inherit;"

        consoleLog(logPrefix, style, resetStyle)

        if (throwable != null) {
            println(throwable.stackTraceToString())
        }
    }
}
