package br.com.carvalho.podcast.core.util

import br.com.carvalho.podcast.core.observability.CrashReporter
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLoggerTest {
    private val messages = mutableListOf<String>()
    private val exceptions = mutableListOf<Throwable>()

    @AfterTest
    fun tearDown() {
        AppLogger.crashReporter = null
    }

    @Test
    fun errorsReachTheCrashReporterWithTheirException() {
        AppLogger.crashReporter = object : CrashReporter {
            override fun log(message: String) { messages += message }
            override fun recordException(throwable: Throwable) { exceptions += throwable }
        }
        val failure = IllegalStateException("boom")

        AppLogger.i("Tag", "hello")
        AppLogger.e("Tag", "failed", failure)

        assertEquals(listOf("[INFO] Tag: hello", "[ERROR] Tag: failed"), messages)
        assertEquals(listOf<Throwable>(failure), exceptions)
    }
}
