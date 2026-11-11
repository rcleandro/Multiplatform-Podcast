package br.com.carvalho.podcast.core.observability

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetryCollectionTest {
    private val calls = mutableListOf<String>()

    private val firebase = object : TelemetryCollection {
        override fun setCollectionEnabled(enabled: Boolean) {
            calls += "collection $enabled"
        }

        override fun forgetCollected() {
            calls += "forget"
        }
    }

    // Turning telemetry off stops collection and drops what Firebase still holds about this install (27.3).
    @Test
    fun turningTelemetryOffAlsoForgetsWhatWasCollected() = runTest {
        firebase.followTelemetryConsent(flowOf(true, false, true))

        assertEquals(listOf("collection true", "collection false", "forget", "collection true"), calls)
    }
}
