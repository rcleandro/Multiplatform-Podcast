package br.com.carvalho.podcast.core.util

class FakeNetworkMonitor(var online: Boolean = true) : NetworkMonitor {
    override suspend fun isOnline(): Boolean = online
}
