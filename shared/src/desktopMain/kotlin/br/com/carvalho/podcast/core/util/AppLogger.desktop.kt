package br.com.carvalho.podcast.core.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter

actual object AppLogger {
    private const val RESET = "\u001B[0m"
    private const val RED = "\u001B[31m"
    private const val GREEN = "\u001B[32m"
    private const val CYAN = "\u001B[36m"

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private fun getCurrentTime(): String = LocalTime.now().format(timeFormatter)

    actual fun d(tag: String, message: String) {
        println("${getCurrentTime()} ${CYAN}[DEBUG] [$tag] $message$RESET")
    }

    actual fun i(tag: String, message: String) {
        println("${getCurrentTime()} ${GREEN}[INFO] [$tag] $message$RESET")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("${getCurrentTime()} ${RED}[ERROR] [$tag] $message$RESET")
        throwable?.printStackTrace()
    }
}
