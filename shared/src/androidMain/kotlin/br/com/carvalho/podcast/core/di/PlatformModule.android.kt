package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.observability.FirebaseAnalytics
import br.com.carvalho.podcast.core.observability.FirebaseCrashReporter
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.domain.player.AndroidAudioPlayer
import br.com.carvalho.podcast.domain.player.AudioPlayer
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single(createdAtStart = true) { createAppDatabase(androidContext()) }
    single<AudioPlayer> { AndroidAudioPlayer(androidContext()) }
    single { AppDirectories(FileSystem.SYSTEM, androidContext().filesDir.absolutePath.toPath()) }
    single<Analytics> { FirebaseAnalytics() }
    single<CrashReporter> { FirebaseCrashReporter() }
}
