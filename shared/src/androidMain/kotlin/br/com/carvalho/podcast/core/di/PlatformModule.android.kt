package br.com.carvalho.podcast.core.di

import androidx.work.WorkManager
import br.com.carvalho.podcast.core.download.WorkManagerEpisodeDownloader
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.observability.FirebaseAnalytics
import br.com.carvalho.podcast.core.observability.FirebaseCrashReporter
import br.com.carvalho.podcast.core.observability.whenAllowed
import br.com.carvalho.podcast.core.player.AndroidPlatformPlayer
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.core.util.AndroidNetworkMonitor
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.NetworkMonitor
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import com.russhwolf.settings.Settings
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    // Default store of the platform (SharedPreferences, NSUserDefaults, localStorage), ADR 0006.
    single<Settings> { Settings() }
    single(createdAtStart = true) { createAppDatabase(androidContext(), get()) }
    single<PlatformPlayer> { AndroidPlatformPlayer(androidContext()) }
    single { AppDirectories(FileSystem.SYSTEM, androidContext().filesDir.absolutePath.toPath()) }
    single<Analytics> {
        val preferences = get<PreferencesRepository>()
        FirebaseAnalytics().whenAllowed { preferences.telemetryEnabled.value }
    }
    single<CrashReporter> {
        val preferences = get<PreferencesRepository>()
        FirebaseCrashReporter().whenAllowed { preferences.telemetryEnabled.value }
    }
    single<EpisodeDownloader> { WorkManagerEpisodeDownloader(get(), WorkManager.getInstance(androidContext())) }
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
}
