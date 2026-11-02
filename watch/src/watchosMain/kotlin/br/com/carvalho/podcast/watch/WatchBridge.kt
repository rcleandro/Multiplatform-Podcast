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
import br.com.carvalho.podcast.domain.repository.AgeSignalSource
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.SearchPodcastsUseCase
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

// The screens' facades (25.14b), made for each screen that opens.

fun watchLibrary(): WatchLibrary {
    val koin = KoinPlatform.getKoin()
    return WatchLibrary(koin.get(), koin.get(), koin.get())
}

fun watchPodcast(podcastId: String): WatchPodcast {
    val koin = KoinPlatform.getKoin()
    return WatchPodcast(podcastId, koin.get(), koin.get(), koin.get(), koin.get())
}

fun watchPopular(): WatchPopular {
    val koin = KoinPlatform.getKoin()
    val search = SearchPodcastsUseCase(koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
    return WatchPopular(search, koin.get())
}

private fun documentsDirectory() =
    (NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL)
        .path!!.toPath()
