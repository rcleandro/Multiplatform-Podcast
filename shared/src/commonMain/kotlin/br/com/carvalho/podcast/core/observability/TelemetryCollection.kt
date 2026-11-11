package br.com.carvalho.podcast.core.observability

import kotlinx.coroutines.flow.Flow

/** Firebase's own collection, as each platform reaches it: the Kotlin SDK on Android, Swift on iOS. */
interface TelemetryCollection {
    /** Covers Firebase's automatic events and crashes too, not just what the app logs. */
    fun setCollectionEnabled(enabled: Boolean)

    /**
     * Deletes the crash reports not sent yet and resets the analytics id, so whatever comes after a new "on" is not
     * linked to what was sent before (LGPD Art. 18, VI; 27.3).
     */
    fun forgetCollected()
}

/** Applies the user's choice (16.4), off for Brazilian minors too (26.4); every "off" also forgets. */
suspend fun TelemetryCollection.followTelemetryConsent(enabled: Flow<Boolean>) = enabled.collect { on ->
    setCollectionEnabled(on)
    if (!on) forgetCollected()
}
