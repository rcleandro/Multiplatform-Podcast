package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.LogAnalytics
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.player.WasmAudioPlayer
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.koin.dsl.module

actual val platformModule = module {
    single(createdAtStart = true) { createAppDatabase(get()) }
    single<AudioPlayer> { WasmAudioPlayer() }
    // ponytail: in-memory file system, downloads vanish on reload; roadmap 14.7 hides downloads on the Web.
    single { AppDirectories(FakeFileSystem(), "/".toPath()) }
    single<Analytics> { LogAnalytics() }
}
