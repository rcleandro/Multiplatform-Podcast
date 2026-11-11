package br.com.carvalho.podcast.core.observability

/**
 * Firebase on iOS, implemented in Swift over the Firebase Swift package (25.15). The Kotlin SDK would need the
 * Firebase frameworks at every Kotlin link, tests included, and only Xcode gets them from the package. Every call
 * before [configure] has finished is dropped by the Swift side.
 */
interface FirebaseBridge : TelemetryCollection {
    /** Starts the default Firebase app; called once, off the main thread. */
    fun configure()

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
