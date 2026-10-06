package br.com.carvalho.podcast.core.util

import br.com.carvalho.podcast.core.observability.CrashReporter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class AppLoggerTest {
    private val messages = mutableListOf<String>()
    private val exceptions = mutableListOf<Throwable>()

    @BeforeTest
    fun setUp() {
        AppLogger.crashReporter = object : CrashReporter {
            override fun log(message: String) { messages += message }
            override fun recordException(throwable: Throwable) { exceptions += throwable }
        }
    }

    @AfterTest
    fun tearDown() {
        AppLogger.crashReporter = null
    }

    @Test
    fun errorsReachTheCrashReporterWithTheirException() {
        val failure = IllegalStateException("boom")

        AppLogger.i("Tag", "hello")
        AppLogger.e("Tag", "failed", failure)

        assertEquals(listOf("[INFO] Tag: hello", "[ERROR] Tag: failed"), messages)
        assertSame(failure, exceptions.single())
    }

    @Test
    fun feedUrlsReachTheCrashReporterAsHostOnly() {
        AppLogger.d("Tag", "Fetching $PRIVATE_FEED now")
        AppLogger.e("Tag", "Failed: $PRIVATE_FEED")

        assertEquals(
            listOf("[DEBUG] Tag: Fetching https://feeds.example.com/… now", "[ERROR] Tag: Failed: https://feeds.example.com/…"),
            messages,
        )
    }

    @Test
    fun exceptionsCarryingAFeedUrlAreRecordedWithoutIt() {
        val cause = IllegalStateException("Request timeout has expired [url=$PRIVATE_FEED]")

        AppLogger.e("Tag", "failed", RuntimeException("wrapped", cause))

        val recorded = exceptions.single().stackTraceToString()
        assertFalse(SECRET in recorded, recorded)
        assertFalse("user:pass" in recorded, recorded)
    }

    private companion object {
        const val SECRET = "s3cr3t"
        const val PRIVATE_FEED = "https://user:pass@feeds.example.com/private/rss?token=$SECRET"
    }
}
