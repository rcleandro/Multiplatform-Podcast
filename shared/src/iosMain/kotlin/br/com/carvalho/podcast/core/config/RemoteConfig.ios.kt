package br.com.carvalho.podcast.core.config

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig

actual object RemoteConfig {
    private val firebaseRemoteConfig by lazy { Firebase.remoteConfig }

    actual suspend fun fetchAndActivate(): Boolean {
        return firebaseRemoteConfig.fetchAndActivate()
    }

    actual fun getString(key: String): String {
        return firebaseRemoteConfig.getValue(key).asString()
    }

    actual fun getBoolean(key: String): Boolean {
        return firebaseRemoteConfig.getValue(key).asBoolean()
    }

    actual fun getLong(key: String): Long {
        return firebaseRemoteConfig.getValue(key).asLong()
    }

    actual fun getDouble(key: String): Double {
        return firebaseRemoteConfig.getValue(key).asDouble()
    }
}
