package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.feature.discover.presentation.DiscoverViewModel
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailViewModel
import br.com.carvalho.podcast.feature.library.presentation.LibraryOrderViewModel
import br.com.carvalho.podcast.feature.library.presentation.LibraryViewModel
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.player.presentation.QueueViewModel
import br.com.carvalho.podcast.feature.player.presentation.TranscriptViewModel
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailViewModel
import br.com.carvalho.podcast.feature.search.presentation.SearchViewModel
import br.com.carvalho.podcast.feature.settings.presentation.ListeningStatsViewModel
import br.com.carvalho.podcast.feature.settings.presentation.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import kotlin.test.Test

/**
 * Every view model is created from the app's graph, as a screen does. KoinGraphTest's verify() takes a parameter with
 * a default value as resolved, while viewModelOf asks Koin for every parameter: Settings crashed on opening when one
 * with no definition (`now: () -> Long`) came in, and verify() passed. On the Desktop only: the Android graph needs a
 * Context, and the definitions that went wrong are the common ones.
 */
class KoinViewModelsTest {
    @Test
    fun everyViewModelIsCreatedFromTheGraph() {
        val koin = startKoin { modules(commonModules + platformModule) }.koin
        try {
            VIEW_MODELS.forEach { koin.get<Any>(it, qualifier = null) { parametersOf("id") } }
        } finally {
            stopKoin()
        }
    }

    private companion object {
        val VIEW_MODELS = listOf(
            DiscoverViewModel::class, EpisodeDetailViewModel::class, LibraryOrderViewModel::class, LibraryViewModel::class,
            ListeningStatsViewModel::class, PlayerViewModel::class, PodcastDetailViewModel::class, QueueViewModel::class,
            SearchViewModel::class, SettingsViewModel::class, TranscriptViewModel::class,
        )
    }
}
