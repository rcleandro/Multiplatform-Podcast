package br.com.carvalho.podcast.feature.library.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.delete_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.no_podcasts_found
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_options
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The library screen wired to its real view model, over fakes: opening "Add podcast" and removing a podcast. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class LibraryFlowTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()
    private val audioPlayer = FakeAudioPlayer()

    private fun text(res: StringResource) = runBlocking { getString(res) }

    @BeforeTest
    fun setMain() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel() = LibraryViewModel(
        repository,
        RefreshPodcastUseCase(feedSource, repository),
        DeletePodcastUseCase(repository, FakeEpisodeDownloader()),
        FakePreferencesRepository(),
        PlayEpisodeUseCase(audioPlayer),
        CoroutineDispatchers(main = dispatcher, io = dispatcher, default = dispatcher),
        FakeAnalytics(),
    )

    @Test
    fun theEmptyLibraryOpensAddPodcastAndAPodcastCanBeRemoved() = runComposeUiTest {
        var addOpened = 0
        setContent {
            PodcastTheme {
                LibraryScreen(viewModel = viewModel(), onAddPodcast = { addOpened++ }, onPodcastClick = {})
            }
        }

        onNodeWithText(text(Res.string.no_podcasts_found)).assertExists()
        onNodeWithText(text(Res.string.add_podcast)).performClick()
        assertEquals(1, addOpened)

        // Added elsewhere ("Add podcast"): the library shows it as soon as the database has it.
        repository.podcasts.value = listOf(podcast())
        waitUntil { onAllNodes(hasText(TITLE)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithContentDescription(text(Res.string.podcast_options)).performClick()
        onNodeWithText(text(Res.string.delete_podcast)).performClick()
        onNodeWithText(text(Res.string.delete)).performClick()

        waitUntil { onAllNodes(hasText(text(Res.string.no_podcasts_found))).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun podcast() = Podcast(
        id = FEED_URL, title = TITLE, description = "", imageUrl = null, author = "Alura", language = null,
        categories = emptyList(), feedUrl = FEED_URL, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private companion object {
        const val FEED_URL = "https://feeds.example.com/rss"
        const val TITLE = "Hipsters"
    }
}
