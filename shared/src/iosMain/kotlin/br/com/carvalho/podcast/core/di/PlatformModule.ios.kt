package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.observability.FirebaseAnalytics
import br.com.carvalho.podcast.core.observability.FirebaseCrashReporter
import br.com.carvalho.podcast.core.observability.whenAllowed
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.core.player.IosPlatformPlayer
import br.com.carvalho.podcast.data.download.UrlSessionEpisodeDownloader
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import okio.FileSystem
import okio.Path.Companion.toPath
import com.russhwolf.settings.Settings
import br.com.carvalho.podcast.core.util.IosNetworkMonitor
import br.com.carvalho.podcast.core.util.NetworkMonitor
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual val platformModule = module {
    // Default store of the platform (SharedPreferences, NSUserDefaults, localStorage), ADR 0006.
    single<Settings> { Settings() }
    single(createdAtStart = true) { createAppDatabase(get()) }
    single<PlatformPlayer> { IosPlatformPlayer() }
    single { AppDirectories(FileSystem.SYSTEM, documentsDirectory()) }
    single<Analytics> {
        val preferences = get<PreferencesRepository>()
        FirebaseAnalytics().whenAllowed { preferences.telemetryEnabled.value }
    }
    single<CrashReporter> {
        val preferences = get<PreferencesRepository>()
        FirebaseCrashReporter().whenAllowed { preferences.telemetryEnabled.value }
    }
    single(createdAtStart = true) { UrlSessionEpisodeDownloader(get(), get()) }
    single<EpisodeDownloader> { get<UrlSessionEpisodeDownloader>() }
    single<NetworkMonitor> { IosNetworkMonitor() }
}

private fun documentsDirectory() =
    (NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL)
        .path!!.toPath()
