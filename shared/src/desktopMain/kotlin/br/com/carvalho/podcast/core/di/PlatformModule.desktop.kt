package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.LogAnalytics
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.core.player.DesktopPlatformPlayer
import okio.FileSystem
import okio.Path.Companion.toPath
import br.com.carvalho.podcast.data.download.KtorEpisodeDownloader
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import java.util.prefs.Preferences
import java.io.File

actual val platformModule = module {
    // Its own node: the no-arg Settings() writes to the user root, shared by every Java app (ADR 0006).
    single<Settings> { PreferencesSettings(Preferences.userRoot().node(PREFERENCES_NODE)) }
    single(createdAtStart = true) { createAppDatabase(get()) }
    single<PlatformPlayer> { DesktopPlatformPlayer() }
    single { AppDirectories(FileSystem.SYSTEM, appDirectory()) }
    single<Analytics> { LogAnalytics() }
    single<EpisodeDownloader> { get<KtorEpisodeDownloader>() }
}

private const val PREFERENCES_NODE = "br/com/carvalho/podcast"

private fun appDirectory() = File(System.getProperty("user.home"), ".podcast").apply { mkdirs() }.absolutePath.toPath()
