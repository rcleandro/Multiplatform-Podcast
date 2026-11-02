package br.com.carvalho.podcast.watch

import br.com.carvalho.podcast.core.di.coreModules
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.LogAnalytics
import br.com.carvalho.podcast.core.player.IosPlatformPlayer
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.core.safety.NoAgeSignalSource
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.IosNetworkMonitor
import br.com.carvalho.podcast.core.util.NetworkMonitor
import br.com.carvalho.podcast.data.download.UrlSessionEpisodeDownloader
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.AgeSignalSource
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

// What the watch app's SwiftUI (ADR 0016) asks of the shared code, by hand as in ADR 0015: start, and the flows its
// screens follow.

private var started = false

/** What the watch creates itself: no Firebase, no age signal from a store (ECA Digital protections stay on). */
private val watchPlatformModule = module {
    single<Settings> { Settings() }
    single(createdAtStart = true) { createAppDatabase(get()) }
    single<PlatformPlayer> { IosPlatformPlayer() }
    single { AppDirectories(FileSystem.SYSTEM, documentsDirectory()) }
    single<Analytics> { LogAnalytics() }
    single {
        val preferences = get<PreferencesRepository>()
        UrlSessionEpisodeDownloader(get(), get()) { preferences.wifiOnlyDownloads.value }
    }
    single<EpisodeDownloader> { get<UrlSessionEpisodeDownloader>() }
    single<NetworkMonitor> { IosNetworkMonitor() }
    single<AgeSignalSource> { NoAgeSignalSource }
}

/** Starts the shared code; the watch app calls it once, before its first screen. */
fun startWatch() {
    if (started) return
    started = true
    startKoin { modules(coreModules + watchPlatformModule) }
}

/** What Swift holds to stop watching a flow. */
fun interface Watching {
    fun cancel()
}

/** Calls [onEach] on the main thread with every value of this flow until the handle is cancelled. */
fun <T> Flow<T>.watch(onEach: (T) -> Unit): Watching {
    val job = MainScope().launch { collect { onEach(it) } }
    return Watching { job.cancel() }
}

/** The library's podcasts, in the order the phone keeps. */
fun watchPodcasts(): Flow<List<Podcast>> = KoinPlatform.getKoin().get<PodcastRepository>().getPodcasts()

private fun documentsDirectory() =
    (NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL)
        .path!!.toPath()
