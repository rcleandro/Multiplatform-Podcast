package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.image.createImageLoader
import br.com.carvalho.podcast.core.network.commonJson
import br.com.carvalho.podcast.core.network.createHttpClient
import br.com.carvalho.podcast.data.local.AppDatabase
import br.com.carvalho.podcast.data.local.createAppDatabase
import br.com.carvalho.podcast.data.remote.RssFeedDataSource
import br.com.carvalho.podcast.data.remote.RssFeedDataSourceImpl
import br.com.carvalho.podcast.data.repository.PodcastRepositoryImpl
import br.com.carvalho.podcast.data.repository.PlayerRepositoryImpl
import br.com.carvalho.podcast.domain.player.createAudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.data.download.KtorEpisodeDownloader
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.feature.downloads.presentation.DownloadedEpisodesViewModel
import br.com.carvalho.podcast.feature.search.presentation.SearchViewModel
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailViewModel
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailViewModel
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.library.presentation.LibraryViewModel
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import io.ktor.utils.io.ioDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
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
}

val playerModule = module {
    single { createAudioPlayer() }
}

val databaseModule = module {
    single<AppDatabase>(createdAtStart = true) { createAppDatabase() }
    single { get<AppDatabase>().podcastDao() }
    single { get<AppDatabase>().episodeDao() }
    single { get<AppDatabase>().playbackStateDao() }
}

val repositoryModule = module {
    single<PodcastRepository> { PodcastRepositoryImpl(get(), get()) }
    single<PlayerRepository> { PlayerRepositoryImpl(get()) }
    single<EpisodeDownloader> { KtorEpisodeDownloader(get(), get()) }
}

val useCaseModule = module {
    singleOf(::AddPodcastFromUrlUseCase)
    singleOf(::RefreshPodcastUseCase)
    singleOf(::DeletePodcastUseCase)
}

val viewModelModule = module {
    viewModelOf(::PlayerViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::PodcastDetailViewModel)
    viewModelOf(::EpisodeDetailViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::DownloadedEpisodesViewModel)
}

val commonModules = listOf(
    dispatcherModule,
    networkModule,
    playerModule,
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)
