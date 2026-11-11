package br.com.carvalho.podcast.core.observability

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirebaseBridgeTest {
    private class FakeFirebase : FirebaseBridge {
        val calls = mutableListOf<String>()

        override fun configure() {
            calls += "configure"
        }

        override fun setCollectionEnabled(enabled: Boolean) {
            calls += "collection $enabled"
        }

        override fun forgetCollected() {
            calls += "forget"
        }

        override fun logEvent(name: String, params: Map<String, Any>?) {
            calls += "event $name $params"
        }

        override fun log(message: String) {
            calls += "log $message"
        }

        override fun recordError(type: String, message: String, stack: String) {
            calls += "error $type $message ${stack.isNotEmpty()}"
        }
    }

    // A parameter without a value never reaches Firebase, and an event left with none goes without parameters.
    @Test
    fun eventsReachSwiftWithoutEmptyValues() {
        val firebase = FakeFirebase()
        val analytics = BridgeAnalytics(firebase)

        analytics.logEvent(AnalyticsEvent.LoadEpisodeDetailError(object : Throwable() {}))
        analytics.logEvent(AnalyticsEvent.ClearHistory)
        analytics.logEvent(AnalyticsEvent.ActOnEpisodes("queue", 2))

        assertEquals(
            listOf(
                "event load_episode_detail_error null",
                "event clear_history null",
                "event act_on_episodes {action=queue, count=2}",
            ),
            firebase.calls,
        )
    }

    @Test
    fun caughtExceptionsReachSwiftWithTypeMessageAndStack() {
        val firebase = FakeFirebase()

        BridgeCrashReporter(firebase).recordException(IllegalStateException("feed broke"))
        BridgeCrashReporter(firebase).log("refreshing")

        assertEquals("error kotlin.IllegalStateException feed broke true", firebase.calls[0])
        assertEquals("log refreshing", firebase.calls[1])
    }

    // Turning telemetry off in Settings, or the Brazilian minor rules (26.x), switch Firebase's own collection off.
    @Test
    fun theUsersChoiceReachesFirebaseCollection() = runTest {
        val firebase = FakeFirebase()

        firebase.followTelemetryConsent(flowOf(true, false))

        assertTrue(firebase.calls == listOf("collection true", "collection false", "forget"))
    }
}
