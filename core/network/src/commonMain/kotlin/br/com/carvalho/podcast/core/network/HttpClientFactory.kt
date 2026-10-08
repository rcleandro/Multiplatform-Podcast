package br.com.carvalho.podcast.core.network

import br.com.carvalho.podcast.core.util.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

private const val TAG = "HTTP Client"

expect fun createHttpClient(): HttpClient

object KtorLogger : Logger {
    override fun log(message: String) {
        AppLogger.d(TAG, message)
    }
}

/** Request lines and headers in debug builds only; credentials of private feeds never reach the log. */
fun LoggingConfig.podcastLogging() {
    logger = KtorLogger
    level = if (AppLogger.isDebugBuild) LogLevel.HEADERS else LogLevel.NONE
    sanitizeHeader { it == HttpHeaders.Authorization || it == HttpHeaders.Cookie || it == HttpHeaders.SetCookie }
}

val commonJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
