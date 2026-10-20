package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.image.createImageLoader
import br.com.carvalho.podcast.core.network.commonJson
import br.com.carvalho.podcast.core.network.createHttpClient
import br.com.carvalho.podcast.core.player.PlaybackController
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.data.download.KtorEpisodeDownloader
import br.com.carvalho.podcast.data.local.AppDatabase
import br.com.carvalho.podcast.data.preferences.SettingsPreferencesRepository
import br.com.carvalho.podcast.data.remote.EpisodeChapterSource
import br.com.carvalho.podcast.data.remote.EpisodeTranscriptSource
import br.com.carvalho.podcast.data.remote.ITunesPodcastDirectory
import br.com.carvalho.podcast.data.remote.RssFeedDataSource
import br.com.carvalho.podcast.data.remote.RssFeedDataSourceImpl
import br.com.carvalho.podcast.data.remote.RssFeedSource
import br.com.carvalho.podcast.data.remote.XmlOpmlFormat
import br.com.carvalho.podcast.data.repository.PlayerRepositoryImpl
import br.com.carvalho.podcast.data.repository.PodcastRepositoryImpl
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.player.ChapterSource
import br.com.carvalho.podcast.domain.player.TranscriptSource
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.OpmlFormat
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.repository.PodcastDirectory
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.ActOnEpisodesUseCase
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.AutoDownloadUseCase
import br.com.carvalho.podcast.domain.usecase.ChildSafety
import br.com.carvalho.podcast.domain.usecase.DeleteFinishedDownloadUseCase
import br.com.carvalho.podcast.domain.usecase.DeleteOldDownloadsUseCase
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.domain.usecase.DownloadNextEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.EnqueueEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import br.com.carvalho.podcast.domain.usecase.ExportBackupUseCase
import br.com.carvalho.podcast.domain.usecase.ExportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.FolderSyncUseCase
import br.com.carvalho.podcast.domain.usecase.ImportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.LibrarySyncUseCase
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshInBackgroundUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.domain.usecase.RestoreBackupUseCase
import br.com.carvalho.podcast.feature.discover.discoverFeatureModule
import br.com.carvalho.podcast.feature.episode.episodeFeatureModule
import br.com.carvalho.podcast.feature.library.libraryFeatureModule
import br.com.carvalho.podcast.feature.player.playerFeatureModule
import br.com.carvalho.podcast.feature.podcast.podcastFeatureModule
import br.com.carvalho.podcast.feature.search.searchFeatureModule
import br.com.carvalho.podcast.feature.settings.settingsFeatureModule
import io.ktor.utils.io.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dispatcherModule = module {
    single {
        CoroutineDispatchers(
            main = Dispatchers.Main,
            io = ioDispatcher(),
            default = Dispatchers.Default
        )
    }
}

val networkModule = module {
    single { commonJson }
    single { createHttpClient() }
    single { createImageLoader(get()) }
    single<RssFeedDataSource> { RssFeedDataSourceImpl(get(), get()) }
    single<FeedSource> { RssFeedSource(get()) }
    single<OpmlFormat> { XmlOpmlFormat }
    single<PodcastDirectory> { ITunesPodcastDirectory(get(), get()) }
    single<ChapterSource> { EpisodeChapterSource(get(), get(), get<AppDirectories>().fileSystem, get()) }
    single<TranscriptSource> { EpisodeTranscriptSource(get(), get(), get()) }
}

val databaseModule = module {
    single { get<AppDatabase>().podcastDao() }
    single { get<AppDatabase>().episodeDao() }
    single { get<AppDatabase>().playbackStateDao() }
}

val playerModule = module {
    single<AudioPlayer> {
        val deleteFinished = DeleteFinishedDownloadUseCase(get(), get(), get())
        val downloadNext = DownloadNextEpisodeUseCase(get(), get(), get())
        // Delete first: the freed space counts toward the download limit.
        PlaybackController(
            get(),
            get(),
            get(),
            get(),
            get(),
            onFinished = { finished, queue ->
                deleteFinished(finished)
                downloadNext(finished, queue)
            },
            skipIntervals = get<PreferencesRepository>().skipIntervals,
            chapterSource = get(),
            explicitRule = get<ExplicitContent>().rule,
        )
    }
}

val repositoryModule = module {
    single<PodcastRepository> { PodcastRepositoryImpl(get(), get()) }
    single<PlayerRepository> { PlayerRepositoryImpl(get(), get()) }
    // Settings comes from each platform module (ADR 0006).
    single<PreferencesRepository> {
        // In Brazil, unchosen options start in the protective position (26.4): someone there is unknown until signed.
        SettingsPreferencesRepository(get(), protectiveDefaults = get<ChildSafety>().state.value.inBrazil)
    }
    // Each platform binds EpisodeDownloader: Android and iOS wrap this one to download in the background.
    single {
        val preferences = get<PreferencesRepository>()
        KtorEpisodeDownloader(get(), get(), get(), storageLimitBytes = { preferences.downloadLimitBytes.value })
    }
}

val useCaseModule = module {
    // Brazil's ECA Digital (26.2): one per app, read by whatever the age changes.
    single { ChildSafety(get()) }
    // Lives as long as the app: the player and every list read it (26.3).
    single { ExplicitContent.following(get(), get(), CoroutineScope(SupervisorJob())) }
    singleOf(::AddPodcastFromUrlUseCase)
    singleOf(::ImportOpmlUseCase)
    singleOf(::RefreshInBackgroundUseCase)
    singleOf(::AutoDownloadUseCase)
    single { DeleteOldDownloadsUseCase(get(), get(), get(), get()) }
    singleOf(::ExportOpmlUseCase)
    singleOf(::ExportBackupUseCase)
    singleOf(::RestoreBackupUseCase)
    singleOf(::LibrarySyncUseCase)
    single { FolderSyncUseCase(get()) }
    singleOf(::RefreshPodcastUseCase)
    singleOf(::DeletePodcastUseCase)
    singleOf(::PlayEpisodeUseCase)
    singleOf(::EnqueueEpisodeUseCase)
    singleOf(::ActOnEpisodesUseCase)
}

val commonModules = listOf(
    dispatcherModule,
    networkModule,
    databaseModule,
    repositoryModule,
    playerModule,
    useCaseModule,
    libraryFeatureModule,
    podcastFeatureModule,
    episodeFeatureModule,
    searchFeatureModule,
    playerFeatureModule,
    discoverFeatureModule,
    settingsFeatureModule
)
