package br.com.carvalho.podcast.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
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

    private suspend fun sentRequest(platform: String?): HttpRequestData {
        lateinit var sent: HttpRequestData
        val client = HttpClient(MockEngine { sent = it; respondOk() }) { podcastDefaults(platform) }
        client.get("https://feeds.example.com/rss")
        return sent
    }
}
