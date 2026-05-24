package br.com.carvalho.podcast.core.config

import br.com.carvalho.podcast.core.util.AppLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig

actual object RemoteConfig {
    private const val TAG = "RemoteConfig"
    private val firebaseRemoteConfig by lazy { Firebase.remoteConfig }

    actual suspend fun fetchAndActivate(): Boolean {
        return try {
            firebaseRemoteConfig.fetchAndActivate().also {
                AppLogger.i(TAG, "fetchAndActivate finalizado. Sucesso/Alterado: $it")
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            AppLogger.e(TAG, "Erro ao fazer fetch do RemoteConfig", e)
            false
        }
    }

    actual fun getString(key: String): String {
        val value = firebaseRemoteConfig.getValue(key).asString()
        AppLogger.d(TAG, "getString(key=$key) -> '$value'")
        return value
    }

    actual fun getBoolean(key: String): Boolean {
        val value = firebaseRemoteConfig.getValue(key).asBoolean()
        AppLogger.d(TAG, "getBoolean(key=$key) -> $value")
        return value
    }

    actual fun getLong(key: String): Long {
        val value = firebaseRemoteConfig.getValue(key).asLong()
        AppLogger.d(TAG, "getLong(key=$key) -> $value")
        return value
    }

    actual fun getDouble(key: String): Double {
        val value = firebaseRemoteConfig.getValue(key).asDouble()
        AppLogger.d(TAG, "getDouble(key=$key) -> $value")
        return value
    }
}
