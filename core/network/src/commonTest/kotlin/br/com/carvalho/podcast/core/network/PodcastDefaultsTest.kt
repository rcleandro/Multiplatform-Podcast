package br.com.carvalho.podcast.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PodcastDefaultsTest {
    @Test
    fun requestsNameTheAppAndAcceptCompression() = runTest {
        val request = sentRequest(platform = "Android")

        assertEquals("PodcastKMP/1.0 (Android)", request.headers[HttpHeaders.UserAgent])
        assertTrue("gzip" in request.headers[HttpHeaders.AcceptEncoding].orEmpty())
    }

    @Test
    fun theBrowserKeepsItsOwnUserAgentAndCompression() = runTest {
        val request = sentRequest(platform = null)

        assertNull(request.headers[HttpHeaders.UserAgent])
        assertNull(request.headers[HttpHeaders.AcceptEncoding])
    }

    @Test
    fun anHttpAddressIsTriedOverHttpsFirst() = runTest {
        val tried = mutableListOf<String>()
        val client = HttpClient(MockEngine { tried += it.url.toString(); respondOk() }) { podcastDefaults("Android") }

        client.get("http://feeds.example.com/rss")

        assertEquals(listOf("https://feeds.example.com/rss"), tried)
    }

    @Test
    fun anHttpAddressFallsBackToClearTextWhenHttpsFails() = runTest {
        val tried = mutableListOf<String>()
        val client = HttpClient(
            MockEngine {
                tried += it.url.toString()
                if (it.url.protocol == URLProtocol.HTTPS) error("TLS handshake failed") else respondOk()
            }
        ) { podcastDefaults("Android") }

        client.get("http://feeds.example.com/rss")

        assertEquals(listOf("https://feeds.example.com/rss", "http://feeds.example.com/rss"), tried)
    }

    @Test
    fun anExplicitPortIsLeftAlone() = runTest {
        val tried = mutableListOf<String>()
        val client = HttpClient(MockEngine { tried += it.url.toString(); respondOk() }) { podcastDefaults("Android") }

        client.get("http://localhost:8080/rss")

        assertEquals(listOf("http://localhost:8080/rss"), tried)
    }

    private suspend fun sentRequest(platform: String?): HttpRequestData {
        lateinit var sent: HttpRequestData
        val client = HttpClient(MockEngine { sent = it; respondOk() }) { podcastDefaults(platform) }
        client.get("https://feeds.example.com/rss")
        return sent
    }
}
