package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
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
    }
}
