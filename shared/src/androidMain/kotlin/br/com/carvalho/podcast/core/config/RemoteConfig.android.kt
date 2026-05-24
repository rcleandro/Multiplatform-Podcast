package br.com.carvalho.podcast.core.config

import br.com.carvalho.podcast.core.util.AppLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig

actual object RemoteConfig {
    private const val TAG = "RemoteConfig"
    private val firebaseRemoteConfig by lazy {
        try {
            Firebase.remoteConfig
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            AppLogger.e(TAG, "Erro ao acessar Firebase Remote Config (provavelmente ambiente de teste)", e)
            null
        }
    }

    actual suspend fun fetchAndActivate(): Boolean {
        return try {
            firebaseRemoteConfig?.fetchAndActivate()?.also {
                AppLogger.i(TAG, "fetchAndActivate finalizado. Sucesso/Alterado: $it")
            } ?: false
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            AppLogger.e(TAG, "Erro ao fazer fetch do RemoteConfig", e)
            false
        }
    }

    actual fun getString(key: String): String {
        val value = try {
            firebaseRemoteConfig?.getValue(key)?.asString().orEmpty()
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            ""
        }
        AppLogger.d(TAG, "getString(key=$key) -> '$value'")
        return value
    }

    actual fun getBoolean(key: String): Boolean {
        val value = try {
            firebaseRemoteConfig?.getValue(key)?.asBoolean() ?: false
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            false
        }
        AppLogger.d(TAG, "getBoolean(key=$key) -> $value")
        return value
    }

    actual fun getLong(key: String): Long {
        val value = try {
            firebaseRemoteConfig?.getValue(key)?.asLong() ?: 0L
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            0L
        }
        AppLogger.d(TAG, "getLong(key=$key) -> $value")
        return value
    }

    actual fun getDouble(key: String): Double {
        val value = try {
            firebaseRemoteConfig?.getValue(key)?.asDouble() ?: 0.0
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            0.0
        }
        AppLogger.d(TAG, "getDouble(key=$key) -> $value")
        return value
    }
}
