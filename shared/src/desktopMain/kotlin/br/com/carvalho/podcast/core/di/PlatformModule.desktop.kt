package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.LogAnalytics
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.core.player.DesktopPlatformPlayer
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.dsl.module
import java.io.File

actual val platformModule = module {
    single(createdAtStart = true) { createAppDatabase(get()) }
    single<PlatformPlayer> { DesktopPlatformPlayer() }
    single { AppDirectories(FileSystem.SYSTEM, appDirectory()) }
    single<Analytics> { LogAnalytics() }
}

private fun appDirectory() = File(System.getProperty("user.home"), ".podcast").apply { mkdirs() }.absolutePath.toPath()
