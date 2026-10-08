package br.com.carvalho.podcast.core.observability

import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetryConsentTest {
    private var allowed = true
    private val sent = mutableListOf<String>()

    private val analytics = object : Analytics {
        override fun logEvent(event: AnalyticsEvent) {
            sent += event.name
        }
    }.whenAllowed { allowed }

    private val crashReporter = object : CrashReporter {
        override fun log(message: String) {
            sent += message
        }

        override fun recordException(throwable: Throwable) {
            sent += throwable::class.simpleName.orEmpty()
        }
    }.whenAllowed { allowed }

    @Test
    fun nothingIsSentWhileTelemetryIsOff() {
        allowed = false

        analytics.logEvent(AnalyticsEvent.Pause)
        crashReporter.log("[INFO] Tag: hello")
        crashReporter.recordException(IllegalStateException())

        assertEquals(emptyList(), sent)
    }

    @Test
    fun theChoiceIsReadOnEverySend() {
        analytics.logEvent(AnalyticsEvent.Pause)
        allowed = false
        analytics.logEvent(AnalyticsEvent.Resume)
        allowed = true
        crashReporter.log("back")

        assertEquals(listOf("pause_episode", "back"), sent)
    }
}
