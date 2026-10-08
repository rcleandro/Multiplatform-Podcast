package br.com.carvalho.podcast.core.util

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

/** Reads the first path NWPathMonitor reports, which it does right after starting, then stops it. */
class IosNetworkMonitor : NetworkMonitor {
    override suspend fun isOnline(): Boolean = suspendCancellableCoroutine { continuation ->
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            nw_path_monitor_cancel(monitor)
            if (continuation.isActive) continuation.resume(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
        continuation.invokeOnCancellation { nw_path_monitor_cancel(monitor) }
    }
}
