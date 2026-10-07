package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.observability.FirebaseAnalytics
import br.com.carvalho.podcast.core.observability.FirebaseCrashReporter
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.core.player.AndroidPlatformPlayer
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.core.download.WorkManagerEpisodeDownloader
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import androidx.work.WorkManager
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import com.russhwolf.settings.Settings
import org.koin.dsl.module

actual val platformModule = module {
    // Default store of the platform (SharedPreferences, NSUserDefaults, localStorage), ADR 0006.
    single<Settings> { Settings() }
    single(createdAtStart = true) { createAppDatabase(androidContext(), get()) }
    single<PlatformPlayer> { AndroidPlatformPlayer(androidContext()) }
    single { AppDirectories(FileSystem.SYSTEM, androidContext().filesDir.absolutePath.toPath()) }
    single<Analytics> { FirebaseAnalytics() }
    single<CrashReporter> { FirebaseCrashReporter() }
    single<EpisodeDownloader> { WorkManagerEpisodeDownloader(get(), WorkManager.getInstance(androidContext())) }
}
