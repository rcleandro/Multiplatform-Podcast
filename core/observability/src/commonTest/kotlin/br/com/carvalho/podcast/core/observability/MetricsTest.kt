package br.com.carvalho.podcast.core.observability

import br.com.carvalho.podcast.core.util.AppLogger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class MetricsTest {
    private val messages = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        AppLogger.crashReporter = object : CrashReporter {
            override fun log(message: String) { messages += message }
            override fun recordException(throwable: Throwable) = Unit
        }
    }

    @AfterTest
    fun tearDown() {
        AppLogger.crashReporter = null
    }

    @Test
    fun aMeasurementIsOneStructuredLine() {
        Metrics.record(Metrics.FEED_REFRESH, 1234.milliseconds, "outcome" to "unchanged")

        assertEquals(listOf("[INFO] Metrics: metric=feed_refresh ms=1234 outcome=unchanged"), messages)
    }

    @Test
    fun theAppStartIsRecordedOncePerProcess() {
        Metrics.recordAppStart()
        Metrics.recordAppStart()

        assertEquals(1, messages.count { "metric=app_start" in it }, messages.toString())
    }
}
