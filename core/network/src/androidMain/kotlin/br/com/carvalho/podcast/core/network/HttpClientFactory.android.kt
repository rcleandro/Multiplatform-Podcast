package br.com.carvalho.podcast.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

private const val MAX_RETRIES = 3

// OkHttp, not the Android engine: HttpURLConnection is not thread safe, and cancelling a download closed its stream
// while another thread was reading it ("Unbalanced enter/exit"), which crashed the app.
actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(commonJson)
    }
    install(ContentEncoding) {
        gzip()
        deflate()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = MAX_RETRIES)
        exponentialDelay()
    }
    install(Logging) {
        podcastLogging()
    }
}
