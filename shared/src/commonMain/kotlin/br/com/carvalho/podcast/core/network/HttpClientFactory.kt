package br.com.carvalho.podcast.core.network

import br.com.carvalho.podcast.core.util.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger
import kotlinx.serialization.json.Json

private const val TAG = "HTTP Client"

expect fun createHttpClient(): HttpClient

object KtorLogger : Logger {
    override fun log(message: String) {
        AppLogger.d(TAG, message)
    }
}

val commonJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
