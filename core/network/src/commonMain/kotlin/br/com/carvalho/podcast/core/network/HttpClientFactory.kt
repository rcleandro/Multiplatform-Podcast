package br.com.carvalho.podcast.core.network

import br.com.carvalho.podcast.core.util.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import kotlinx.coroutines.CancellationException
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
    // After the retry, so it runs inside each attempt: a failed https falls back at once instead of being retried.
    install(HttpsFirst)
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

/**
 * An `http://` address is tried as `https://` first and only sent in clear text if that fails (ADR 0009): many hosts
 * already serve both, and the rest still work. Addresses with an explicit port are left alone.
 */
// ponytail: falls back on connection and TLS failures only, not on an https error status; add that if a host
// answers 404 over https and 200 over http.
internal val HttpsFirst = createClientPlugin("HttpsFirst") {
    on(Send) { request ->
        val upgradable = request.url.protocol == URLProtocol.HTTP && request.url.port in DEFAULT_PORTS
        if (!upgradable) return@on proceed(request)
        request.url.protocol = URLProtocol.HTTPS
        request.url.port = 0
        @Suppress("TooGenericExceptionCaught")
        try {
            proceed(request)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            request.url.protocol = URLProtocol.HTTP
            proceed(request)
        }
    }
}

private val DEFAULT_PORTS = setOf(0, URLProtocol.HTTP.defaultPort)

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
