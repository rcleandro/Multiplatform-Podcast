package br.com.carvalho.podcast.core.util

/** Whether the device has a network now; the app opens on the downloads without one (ADR 0005). */
interface NetworkMonitor {
    suspend fun isOnline(): Boolean
}

// ponytail: the Desktop has no network API worth wrapping and the Web has no downloads to fall back to,
// so both count as online; give them a real check if either gets an offline mode.
object AlwaysOnline : NetworkMonitor {
    override suspend fun isOnline(): Boolean = true
}
