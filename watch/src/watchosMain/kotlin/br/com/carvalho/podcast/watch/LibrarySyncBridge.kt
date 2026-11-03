package br.com.carvalho.podcast.watch

import br.com.carvalho.podcast.domain.usecase.DeviceSync
import org.koin.mp.KoinPlatform

// The watch's side of the sync with the iPhone (25.14d), under the same names as the phone's
// (shared/src/iosMain/.../LibrarySyncBridge.kt), so LibrarySync.swift serves both apps.

/** Calls [onSnapshot] with this device's snapshot, as JSON, each time it settles on something new. */
fun followLibrary(onSnapshot: (String) -> Unit): Watching = deviceSync().outgoing().watch(onSnapshot)

/** Merges the other device's snapshot. */
suspend fun receiveLibrary(text: String) {
    deviceSync().receive(text)
}

private fun deviceSync() = DeviceSync(KoinPlatform.getKoin().get())
