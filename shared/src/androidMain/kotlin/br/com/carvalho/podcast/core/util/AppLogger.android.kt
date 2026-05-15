package br.com.carvalho.podcast.core.util

import android.util.Log

actual object AppLogger {
    actual fun d(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Exception) {
            println("[DEBUG] $tag: $message")
        }
    }

    actual fun i(tag: String, message: String) {
        try {
            Log.i(tag, message)
        } catch (_: Exception) {
            println("[INFO] $tag: $message")
        }
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        try {
            Log.e(tag, message, throwable)
        } catch (_: Exception) {
            println("[ERROR] $tag: $message")
            throwable?.printStackTrace()
        }
    }
}
