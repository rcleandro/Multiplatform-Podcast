package br.com.carvalho.podcast.core.sync

import br.com.carvalho.podcast.domain.usecase.DeviceSync
import br.com.carvalho.podcast.presentation.Watching
import br.com.carvalho.podcast.presentation.watch
import org.koin.mp.KoinPlatform

// The iPhone's side of the sync with the Apple Watch (25.14d), for LibrarySync.swift, which the watch app shares under
// the same names (watch/src/watchosMain/.../LibrarySyncBridge.kt).

/** Calls [onSnapshot] with this device's snapshot, as JSON, each time it settles on something new. */
fun followLibrary(onSnapshot: (String) -> Unit): Watching = deviceSync().outgoing().watch(onSnapshot)

/** Merges the other device's snapshot. */
suspend fun receiveLibrary(text: String) {
    deviceSync().receive(text)
}

private fun deviceSync() = DeviceSync(KoinPlatform.getKoin().get())
