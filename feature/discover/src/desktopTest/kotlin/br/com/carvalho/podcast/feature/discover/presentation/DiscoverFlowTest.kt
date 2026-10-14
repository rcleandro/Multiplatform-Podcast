package br.com.carvalho.podcast.feature.discover.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.subscribe_to
import br.com.carvalho.podcast.core.ui.generated.resources.subscribed
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakeOpmlFormat
import br.com.carvalho.podcast.domain.repository.FakePodcastDirectory
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.ImportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.OpenPodcastLinkUseCase
import br.com.carvalho.podcast.domain.usecase.SearchPodcastsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** "Add podcast" wired to its real view model, over fakes: search the directory and subscribe to a result. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DiscoverFlowTest {
    private val directory = FakePodcastDirectory()
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()

    private fun text(res: StringResource, vararg args: Any) = runBlocking { getString(res, *args) }

    // Real time: the search waits for the user to stop typing, and the test waits for the results.
    @BeforeTest
    fun setMain() = Dispatchers.setMain(Dispatchers.Default)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel() = DiscoverViewModel(
        SearchPodcastsUseCase(directory, repository, FakePreferencesRepository()),
        AddPodcastFromUrlUseCase(feedSource, repository),
        OpenPodcastLinkUseCase(directory, feedSource, repository),
        ImportOpmlUseCase(FakeOpmlFormat, AddPodcastFromUrlUseCase(feedSource, repository)),
        CoroutineDispatchers(main = Dispatchers.Default, io = Dispatchers.Default, default = Dispatchers.Default),
        FakeAnalytics(),
    )

    @Test
    fun aPodcastFoundByNameIsSubscribedWithATap() = runComposeUiTest {
        directory.result = Result.success(
            listOf(DirectoryPodcast(TITLE, "Alura", null, FEED, "Tecnologia", 629))
        )
        feedSource.result = Result.success(FetchedFeed(podcast(), emptyList()))
        setContent { PodcastTheme { DiscoverScreen(onBack = {}, viewModel = viewModel()) } }

        onNode(hasSetTextAction()).performTextInput("hipsters")
        waitUntil(timeoutMillis = WAIT_MS) { onAllNodes(hasText(TITLE)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription(text(Res.string.subscribe_to, TITLE)).performClick()

        waitUntil(timeoutMillis = WAIT_MS) {
            onAllNodes(hasContentDescription(text(Res.string.subscribed))).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(FEED, repository.podcasts.value.single().feedUrl)
    }

    private fun podcast() = Podcast(
        id = FEED, title = TITLE, description = "", imageUrl = null, author = "Alura", language = null,
        categories = emptyList(), feedUrl = FEED, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private companion object {
        const val TITLE = "Hipsters Ponto Tech"
        const val FEED = "https://hipsters.tech/feed/podcast/"
        const val WAIT_MS = 5_000L
    }
}
