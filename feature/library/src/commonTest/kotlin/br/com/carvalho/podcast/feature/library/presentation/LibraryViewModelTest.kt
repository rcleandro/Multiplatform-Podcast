package br.com.carvalho.podcast.feature.library.presentation

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.ui.generated.resources.error_invalid_feed
import br.com.carvalho.podcast.core.ui.generated.resources.error_refresh_some_podcasts
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.core.ui.generated.resources.error_podcast_exists
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import app.cash.turbine.test
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import br.com.carvalho.podcast.presentation.UiMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()
    private val addPodcastUseCase = AddPodcastFromUrlUseCase(feedSource, repository)
    private val refreshPodcastUseCase = RefreshPodcastUseCase(feedSource, repository)
    private val deletePodcastUseCase = DeletePodcastUseCase(repository, FakeEpisodeDownloader())
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val analytics = FakeAnalytics()

    private fun createViewModel(): LibraryViewModel {
        return LibraryViewModel(repository, addPodcastUseCase, refreshPodcastUseCase, deletePodcastUseCase, dispatchers, analytics)
    }

    @Test
    fun `initial state is correct`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.podcasts.isEmpty())
            assertFalse(state.isAddDialogOpen)
        }
    }

    @Test
    fun `onAddClicked opens dialog`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onIntent(LibraryIntent.OpenAddDialog)
            assertTrue(awaitItem().isAddDialogOpen)
        }
    }

    @Test
    fun `addPodcast calls use case and closes dialog`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val podcast = Podcast(
            id = "https://test-url", title = "New", description = "", imageUrl = null, author = null, language = null,
            categories = emptyList(), feedUrl = "https://test-url", siteUrl = null, lastUpdated = 0, isSubscribed = true
        )
        feedSource.result = Result.success(FetchedFeed(podcast, emptyList()))
        feedSource.delayMs = 10

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(LibraryIntent.OpenAddDialog)
            awaitItem()

            viewModel.onIntent(LibraryIntent.ChangeUrl("test-url"))
            awaitItem()

            viewModel.onIntent(LibraryIntent.ConfirmAdd)
            
            var state = awaitItem()
            while (!state.isRefreshing) {
                state = awaitItem()
            }
            assertTrue(state.isRefreshing)
            assertFalse(state.isAddDialogOpen)

            state = awaitItem()
            while (state.isRefreshing) {
                state = awaitItem()
            }
            assertFalse(state.isRefreshing)
            assertEquals("", state.addUrl)
            
            assertEquals("https://test-url", feedSource.fetchCalledWith)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirmDelete calls delete use case`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val podcast = Podcast(id = "1", title = "P1", description = "", imageUrl = null, author = null, language = null, categories = emptyList(), feedUrl = "", siteUrl = null, lastUpdated = 0, isSubscribed = true)
        
        viewModel.uiState.test {
            awaitItem()
            
            viewModel.onIntent(LibraryIntent.RequestDelete(podcast))
            awaitItem()

            viewModel.onIntent(LibraryIntent.ConfirmDelete)
            
            val state = awaitItem()
            assertEquals(null, state.podcastToDelete)
            
            assertEquals("1", repository.deletePodcastCalledWith)
            assertEquals(listOf("delete_podcast"), analytics.events)
        }
    }

    @Test
    fun `events never carry the feed URL or titles`() = runTest(testDispatcher) {
        val feedUrl = "https://feeds.example.com/private/rss?token=s3cr3t"
        val podcast = Podcast(
            id = feedUrl, title = "Paid show", description = "", imageUrl = null, author = null, language = null,
            categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true
        )
        feedSource.result = Result.success(FetchedFeed(podcast, emptyList()))
        val viewModel = createViewModel()

        viewModel.onIntent(LibraryIntent.ChangeUrl(feedUrl))
        viewModel.onIntent(LibraryIntent.ConfirmAdd)
        viewModel.onIntent(LibraryIntent.RequestDelete(podcast))
        viewModel.onIntent(LibraryIntent.ConfirmDelete)

        assertEquals(listOf("add_podcast_attempt", "add_podcast_success", "delete_podcast"), analytics.events)
        analytics.params.forEach { params ->
            assertEquals(mapOf<String, Any?>("host" to "feeds.example.com"), params)
        }
    }

    @Test
    fun `adding a podcast already in the library shows a specific message`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(
            Podcast(
                id = "https://feed.example/rss", title = "Existing", description = "", imageUrl = null, author = null,
                language = null, categories = emptyList(), feedUrl = "https://feed.example/rss", siteUrl = null,
                lastUpdated = 0, isSubscribed = true
            )
        )
        val viewModel = createViewModel()

        viewModel.onIntent(LibraryIntent.ChangeUrl("https://feed.example/rss"))
        viewModel.onIntent(LibraryIntent.ConfirmAdd)

        viewModel.messages.test {
            assertEquals(UiMessage(Res.string.error_podcast_exists), awaitItem())
        }
    }

    @Test
    fun `adding a URL that is not a feed says so`() = runTest(testDispatcher) {
        feedSource.result = Result.failure(AppError.InvalidFeed)
        val viewModel = createViewModel()

        viewModel.onIntent(LibraryIntent.ChangeUrl("https://example.com"))
        viewModel.onIntent(LibraryIntent.ConfirmAdd)

        viewModel.messages.test {
            assertEquals(UiMessage(Res.string.error_invalid_feed), awaitItem())
        }
    }

    @Test
    fun `refreshing all when some feeds fail says how many`() = runTest(testDispatcher) {
        val podcasts = listOf("a", "b", "c").map {
            Podcast(
                id = it, title = it, description = "", imageUrl = null, author = null, language = null,
                categories = emptyList(), feedUrl = it, siteUrl = null, lastUpdated = 0, isSubscribed = true
            )
        }
        repository.podcasts.value = podcasts
        feedSource.result = Result.success(FetchedFeed(podcasts.first(), emptyList()))
        feedSource.resultsByUrl["b"] = Result.failure(AppError.NoConnection)
        val viewModel = createViewModel()

        viewModel.onIntent(LibraryIntent.RefreshAll)

        viewModel.messages.test {
            assertEquals(UiMessage(Res.string.error_refresh_some_podcasts, listOf(1, 3)), awaitItem())
        }
    }
}
