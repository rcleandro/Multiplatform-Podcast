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
import io.ktor.client.engine.HttpClientEngine
import kotlin.test.Test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.verify.verify

/** Fails when a definition needs a type nothing binds, instead of crashing when the screen opens. */
class KoinGraphTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyDefinitionCanBeResolved() {
        module { includes(commonModules + platformModule) }.verify(extraTypes = EXTRA_TYPES)
    }

    // verify() takes a parameter with a default value as resolved, while viewModelOf asks Koin for every parameter:
    // Settings crashed on opening when one with no definition (`now: () -> Long`) came in, and verify() passed.
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

        /** Built inside definition lambdas from values Koin does not provide. */
        val EXTRA_TYPES = listOf(
            CoroutineDispatcher::class,
            CoroutineScope::class,
            HttpClientEngine::class,
            FileSystem::class,
            Path::class,
        )
    }
}
