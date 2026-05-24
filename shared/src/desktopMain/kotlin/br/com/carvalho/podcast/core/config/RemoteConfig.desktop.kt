package br.com.carvalho.podcast.core.config

actual object RemoteConfig {
    actual suspend fun fetchAndActivate(): Boolean = false
    actual fun getString(key: String): String = ""
    actual fun getBoolean(key: String): Boolean = false
    actual fun getLong(key: String): Long = 0L
    actual fun getDouble(key: String): Double = 0.0
}
