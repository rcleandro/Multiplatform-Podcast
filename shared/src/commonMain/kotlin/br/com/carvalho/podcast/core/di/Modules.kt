package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.image.createImageLoader
import br.com.carvalho.podcast.core.network.commonJson
import br.com.carvalho.podcast.core.network.createHttpClient
import br.com.carvalho.podcast.core.player.PlaybackController
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.data.download.KtorEpisodeDownloader
import br.com.carvalho.podcast.data.local.AppDatabase
import br.com.carvalho.podcast.data.preferences.SettingsPreferencesRepository
import br.com.carvalho.podcast.data.remote.RssFeedDataSource
import br.com.carvalho.podcast.data.remote.RssFeedDataSourceImpl
import br.com.carvalho.podcast.data.remote.RssFeedSource
import br.com.carvalho.podcast.data.repository.PlayerRepositoryImpl
import br.com.carvalho.podcast.data.repository.PodcastRepositoryImpl
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.feature.episode.episodeFeatureModule
import br.com.carvalho.podcast.feature.library.libraryFeatureModule
import br.com.carvalho.podcast.feature.player.playerFeatureModule
import br.com.carvalho.podcast.feature.podcast.podcastFeatureModule
import br.com.carvalho.podcast.feature.search.searchFeatureModule
import io.ktor.utils.io.ioDispatcher
import kotlinx.coroutines.Dispatchers
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
}

val databaseModule = module {
    single { get<AppDatabase>().podcastDao() }
    single { get<AppDatabase>().episodeDao() }
    single { get<AppDatabase>().playbackStateDao() }
}

val playerModule = module {
    single<AudioPlayer> { PlaybackController(get(), get(), get(), get(), get()) }
}

val repositoryModule = module {
    single<PodcastRepository> { PodcastRepositoryImpl(get(), get()) }
    single<PlayerRepository> { PlayerRepositoryImpl(get()) }
    // Settings comes from each platform module (ADR 0006).
    single<PreferencesRepository> { SettingsPreferencesRepository(get()) }
    // Each platform binds EpisodeDownloader: Android and iOS wrap this one to download in the background.
    single { KtorEpisodeDownloader(get(), get(), get()) }
}

val useCaseModule = module {
    singleOf(::AddPodcastFromUrlUseCase)
    singleOf(::RefreshPodcastUseCase)
    singleOf(::DeletePodcastUseCase)
    singleOf(::PlayEpisodeUseCase)
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
    playerFeatureModule
)
