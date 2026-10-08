package br.com.carvalho.podcast.core.network

import br.com.carvalho.podcast.core.util.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TAG = "HTTP Client"
private const val MAX_RETRIES = 3
private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 15_000L
private const val SOCKET_TIMEOUT_MS = 30_000L

// ponytail: the version is fixed until phase 20 adds one version for every app; read it from there then.
private const val APP_VERSION = "1.0"

/** Each platform only picks its engine and calls [podcastDefaults]. */
expect fun createHttpClient(): HttpClient

/**
 * Timeouts, retry, logging, compression and a User-Agent naming the app, shared by every platform. Some podcast
 * hosts refuse unnamed clients (Buzzsprout answers 403 to `ktor-client` and `okhttp`). [platform] is null in the
 * browser, which compresses and names itself: setting either from a page is refused or triggers a CORS preflight.
 */
fun HttpClientConfig<*>.podcastDefaults(platform: String?) {
    install(ContentNegotiation) {
        json(commonJson)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = MAX_RETRIES)
        exponentialDelay()
    }
    install(Logging) {
        podcastLogging()
    }
    if (platform != null) {
        install(ContentEncoding) {
            gzip()
            deflate()
        }
        install(UserAgent) {
            agent = "PodcastKMP/$APP_VERSION ($platform)"
        }
    }
}

object KtorLogger : Logger {
    override fun log(message: String) {
        AppLogger.d(TAG, message)
    }
}

/** Request lines and headers in debug builds only; credentials of private feeds never reach the log. */
private fun LoggingConfig.podcastLogging() {
    logger = KtorLogger
    level = if (AppLogger.isDebugBuild) LogLevel.HEADERS else LogLevel.NONE
    sanitizeHeader { it == HttpHeaders.Authorization || it == HttpHeaders.Cookie || it == HttpHeaders.SetCookie }
}

val commonJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
