package br.com.carvalho.podcast.core.crashlytics

import co.touchlab.kermit.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import kotlin.getValue

actual object Crashlytics {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private val firebaseCrashlytics by lazy {
        try {
            Firebase.crashlytics
        } catch (_: Exception) {
            // Em testes unitários Android (HostTest), o FirebaseApp não está inicializado.
            null
        }
    }

    actual fun log(message: String) {
        firebaseCrashlytics?.log(message)
    }

    actual fun recordException(throwable: Throwable) {
        firebaseCrashlytics?.recordException(throwable)
    }

    actual fun setUserId(userId: String) {
        firebaseCrashlytics?.setUserId(userId)
    }

    actual fun setCustomKey(key: String, value: String) {
        firebaseCrashlytics?.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Int) {
        firebaseCrashlytics?.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Boolean) {
        firebaseCrashlytics?.setCustomKey(key, value)
    }
}
