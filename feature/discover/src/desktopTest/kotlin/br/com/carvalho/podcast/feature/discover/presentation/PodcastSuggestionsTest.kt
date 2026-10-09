package br.com.carvalho.podcast.feature.discover.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.subscribe_to
import br.com.carvalho.podcast.core.ui.generated.resources.subscribed
import br.com.carvalho.podcast.core.ui.generated.resources.suggestions_title
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastDirectory
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.OpenPodcastLinkUseCase
import br.com.carvalho.podcast.domain.usecase.SearchPodcastsUseCase
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

/** The suggestions an empty library shows (18.3), wired to their real view model over fakes. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PodcastSuggestionsTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val directory = FakePodcastDirectory()
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()

    private fun text(res: StringResource, vararg args: Any) = runBlocking { getString(res, *args) }

    @BeforeTest
    fun setMain() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel() = DiscoverViewModel(
        SearchPodcastsUseCase(directory, repository, FakePreferencesRepository()),
        AddPodcastFromUrlUseCase(feedSource, repository),
        OpenPodcastLinkUseCase(directory, feedSource, repository),
        CoroutineDispatchers(main = dispatcher, io = dispatcher, default = dispatcher),
        FakeAnalytics(),
    )

    @Test
    fun anEmptyLibraryOffersThePopularPodcastsToSubscribe() = runComposeUiTest {
        directory.topResult = Result.success(listOf(DirectoryPodcast(TITLE, "Alura", null, FEED, "Tecnologia", 629)))
        feedSource.result = Result.success(FetchedFeed(podcast(), emptyList()))
        var addOpened = 0
        setContent {
            PodcastTheme { PodcastSuggestions(onAddPodcast = { addOpened++ }, viewModel = viewModel()) }
        }

        onNodeWithText(text(Res.string.suggestions_title)).assertExists()
        onNodeWithContentDescription(text(Res.string.subscribe_to, TITLE)).performClick()
        waitUntil { onAllNodes(hasContentDescription(text(Res.string.subscribed))).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(FEED, repository.podcasts.value.single().feedUrl)

        onNode(hasText(text(Res.string.add_podcast))).performClick()
        assertEquals(1, addOpened)
    }

    private fun podcast() = Podcast(
        id = FEED, title = TITLE, description = "", imageUrl = null, author = "Alura", language = null,
        categories = emptyList(), feedUrl = FEED, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private companion object {
        const val TITLE = "Hipsters Ponto Tech"
        const val FEED = "https://hipsters.tech/feed/podcast/"
    }
}
