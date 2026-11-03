package br.com.carvalho.podcast.core.observability

import kotlinx.coroutines.flow.Flow

/**
 * Firebase on iOS, implemented in Swift over the Firebase Swift package (25.15). The Kotlin SDK would need the
 * Firebase frameworks at every Kotlin link, tests included, and only Xcode gets them from the package. Every call
 * before [configure] has finished is dropped by the Swift side.
 */
interface FirebaseBridge {
    /** Starts the default Firebase app; called once, off the main thread. */
    fun configure()

    /** The user's choice for Firebase's own collection; off also deletes the reports not sent yet. */
    fun setCollectionEnabled(enabled: Boolean)

    fun logEvent(name: String, params: Map<String, Any>?)

    fun log(message: String)

    /** A caught exception: its type, its message and its stack as Kotlin prints it. */
    fun recordError(type: String, message: String, stack: String)
}

internal class BridgeAnalytics(private val firebase: FirebaseBridge) : Analytics {
    override fun logEvent(event: AnalyticsEvent) {
        val values = event.params.filterValues { it != null }.mapValues { it.value!! }
        firebase.logEvent(event.name, values.ifEmpty { null })
    }
}

internal class BridgeCrashReporter(private val firebase: FirebaseBridge) : CrashReporter {
    override fun log(message: String) = firebase.log(message)

    override fun recordException(throwable: Throwable) = firebase.recordError(
        type = throwable::class.qualifiedName ?: "Throwable",
        message = throwable.message.orEmpty(),
        stack = throwable.stackTraceToString(),
    )
}

/** Applies the user's choice to Firebase's collection, which also covers its automatic events and crashes (16.4). */
internal suspend fun FirebaseBridge.followTelemetryConsent(enabled: Flow<Boolean>) =
    enabled.collect(::setCollectionEnabled)
