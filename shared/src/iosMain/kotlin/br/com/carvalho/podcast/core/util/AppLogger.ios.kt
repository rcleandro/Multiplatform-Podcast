package br.com.carvalho.podcast.core.util

import platform.Foundation.NSLog

actual object AppLogger {
    actual fun d(tag: String, message: String) {
        NSLog("DEBUG: [$tag] $message")
    }

    actual fun i(tag: String, message: String) {
        NSLog("INFO: [$tag] $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val errorMessage = if (throwable != null) {
            "$message - Error: ${throwable.message}\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        NSLog("ERROR: [$tag] $errorMessage")
    }
}
