package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.FeedVersion
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RssFeedDataSourceImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher, default = testDispatcher)

    private fun dataSource(engine: MockEngine) = RssFeedDataSourceImpl(HttpClient(engine), dispatchers)

    @Test
    fun `an HTTP error becomes AppError Http with the status`() = runTest(testDispatcher) {
        val result = dataSource(MockEngine { respondError(HttpStatusCode.NotFound) }).fetchFeed(FEED_URL)

        assertEquals(AppError.Http(HttpStatusCode.NotFound.value), result.exceptionOrNull())
    }

    @Test
    fun `a web page instead of a feed becomes AppError InvalidFeed`() = runTest(testDispatcher) {
        val result = dataSource(MockEngine { respond("<html><body>Hi</body></html>") }).fetchFeed(FEED_URL)

        assertEquals(AppError.InvalidFeed, result.exceptionOrNull())
    }

    @Test
    fun `a conditional fetch sends the version and reads not modified as no feed`() = runTest(testDispatcher) {
        var headers: Headers? = null
        val engine = MockEngine { request ->
            headers = request.headers
            respond("", HttpStatusCode.NotModified)
        }

        val result = dataSource(engine).fetchFeed(FEED_URL, FeedVersion(etag = ETAG, lastModified = LAST_MODIFIED))

        assertEquals(null, result.getOrThrow())
        assertEquals(ETAG, headers?.get(HttpHeaders.IfNoneMatch))
        assertEquals(LAST_MODIFIED, headers?.get(HttpHeaders.IfModifiedSince))
    }

    @Test
    fun `a fetched feed carries the version the server sent`() = runTest(testDispatcher) {
        val engine = MockEngine {
            respond(
                "<rss><channel><title>T</title></channel></rss>",
                headers = headersOf(HttpHeaders.ETag to listOf(ETAG), HttpHeaders.LastModified to listOf(LAST_MODIFIED)),
            )
        }

        val feed = dataSource(engine).fetchFeed(FEED_URL).getOrThrow()

        assertEquals(FeedVersion(etag = ETAG, lastModified = LAST_MODIFIED), feed?.version)
    }

    @Test
    fun `a permanent redirect is followed and reported as the new address`() = runTest(testDispatcher) {
        val engine = MockEngine { request ->
            when (request.url.toString()) {
                FEED_URL -> respondRedirect(HttpStatusCode.MovedPermanently, "/moved/rss")
                "https://feed.example/moved/rss" -> respondRedirect(HttpStatusCode.PermanentRedirect, NEW_URL)
                else -> respond(FEED)
            }
        }

        val feed = dataSource(engine).fetchFeed(FEED_URL).getOrThrow()

        assertEquals("T", feed?.title)
        assertEquals(NEW_URL, feed?.permanentRedirect)
    }

    @Test
    fun `a temporary redirect anywhere on the way is not a new address`() = runTest(testDispatcher) {
        val engine = MockEngine { request ->
            when (request.url.toString()) {
                FEED_URL -> respondRedirect(HttpStatusCode.MovedPermanently, "https://cdn.example/rss")
                "https://cdn.example/rss" -> respondRedirect(HttpStatusCode.Found, NEW_URL)
                else -> respond(FEED)
            }
        }

        val feed = dataSource(engine).fetchFeed(FEED_URL).getOrThrow()

        assertEquals("T", feed?.title)
        assertEquals(null, feed?.permanentRedirect)
    }

    @Test
    fun `a redirect loop stops`() = runTest(testDispatcher) {
        var requests = 0
        val engine = MockEngine {
            requests++
            respondRedirect(HttpStatusCode.MovedPermanently, FEED_URL)
        }

        val result = dataSource(engine).fetchFeed(FEED_URL)

        assertEquals(AppError.Http(HttpStatusCode.MovedPermanently.value), result.exceptionOrNull())
        assertEquals(6, requests)
    }

    private fun MockRequestHandleScope.respondRedirect(status: HttpStatusCode, location: String) =
        respond("", status, headersOf(HttpHeaders.Location, location))

    @Test
    fun `a network failure becomes AppError NoConnection`() = runTest(testDispatcher) {
        val result = dataSource(MockEngine { throw kotlinx.io.IOException("unreachable") }).fetchFeed(FEED_URL)

        assertEquals(AppError.NoConnection, result.exceptionOrNull())
    }

    @Test
    fun `a full disk becomes AppError StorageFull and anything else AppError Unknown`() {
        assertEquals(AppError.StorageFull, kotlinx.io.IOException("write failed: ENOSPC").toAppError())
        assertIs<AppError.Unknown>(IllegalStateException("bug").toAppError())
    }

    private companion object {
        const val FEED_URL = "https://feed.example/rss"
        const val NEW_URL = "https://new.example/rss"
        const val FEED = "<rss><channel><title>T</title></channel></rss>"
        const val ETAG = "\"abc123\""
        const val LAST_MODIFIED = "Mon, 05 Oct 2026 10:00:00 GMT"
    }
}
