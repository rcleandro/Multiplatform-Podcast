package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.observability.FirebaseAnalytics
import br.com.carvalho.podcast.core.observability.FirebaseCrashReporter
import br.com.carvalho.podcast.core.observability.whenAllowed
import br.com.carvalho.podcast.core.player.IosPlatformPlayer
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.core.safety.StoreAgeSignalSource
import br.com.carvalho.podcast.core.safety.telemetryAllowed
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.IosNetworkMonitor
import br.com.carvalho.podcast.core.util.NetworkMonitor
import br.com.carvalho.podcast.data.download.UrlSessionEpisodeDownloader
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.repository.AgeSignalSource
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.ChildSafety
import com.russhwolf.settings.Settings
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.numberWithBool

actual val platformModule = module {
    // Default store of the platform (SharedPreferences, NSUserDefaults, localStorage), ADR 0006.
    single<Settings> { Settings() }
    single(createdAtStart = true) { createAppDatabase(get()) }
    single<PlatformPlayer> { IosPlatformPlayer() }
    single { AppDirectories(FileSystem.SYSTEM, documentsDirectory()).also { excludeFromBackup(it.downloadsDir) } }
    single<Analytics> {
        val preferences = get<PreferencesRepository>()
        val childSafety = get<ChildSafety>()
        FirebaseAnalytics().whenAllowed { telemetryAllowed(preferences, childSafety) }
    }
    single<CrashReporter> {
        val preferences = get<PreferencesRepository>()
        val childSafety = get<ChildSafety>()
        FirebaseCrashReporter().whenAllowed { telemetryAllowed(preferences, childSafety) }
    }
    single(createdAtStart = true) {
        val preferences = get<PreferencesRepository>()
        UrlSessionEpisodeDownloader(get(), get()) { preferences.wifiOnlyDownloads.value }
    }
    single<EpisodeDownloader> { get<UrlSessionEpisodeDownloader>() }
    single<NetworkMonitor> { IosNetworkMonitor() }
    single<AgeSignalSource> { StoreAgeSignalSource }
}

private fun documentsDirectory() =
    (NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL)
        .path!!.toPath()

/**
 * Keeps [folder] out of iCloud and computer backups (22.7): downloaded episodes come back from the feed, and App Review
 * turns down apps that back up content they can download again. Set at every start, on the folder itself, so it also
 * covers what is saved in it later.
 */
// ponytail: a folder deleted and made again while the app runs is unmarked until the next start.
@OptIn(ExperimentalForeignApi::class)
internal fun excludeFromBackup(folder: Path) {
    FileSystem.SYSTEM.createDirectories(folder)
    NSURL.fileURLWithPath(folder.toString())
        .setResourceValue(NSNumber.numberWithBool(true), forKey = NSURLIsExcludedFromBackupKey, error = null)
}
