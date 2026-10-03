package br.com.carvalho.podcast.feature.library.presentation

import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.shared.error_podcast_exists
import br.com.carvalho.podcast.shared.Res
import app.cash.turbine.test
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
    private val deletePodcastUseCase = DeletePodcastUseCase(repository)
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
            viewModel.onAddClicked()
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

            viewModel.onAddClicked()
            awaitItem()

            viewModel.onUrlChanged("test-url")
            awaitItem()

            viewModel.addPodcast()
            
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
            
            viewModel.onDeleteClicked(podcast)
            awaitItem()

            viewModel.confirmDelete()
            
            val state = awaitItem()
            assertEquals(null, state.podcastToDelete)
            
            assertEquals("1", repository.deletePodcastCalledWith)
            assertEquals(listOf("delete_podcast"), analytics.events)
        }
    }

    @Test
    fun `adding a podcast already in the library shows a specific message`() = runTest(testDispatcher) {
        repository.savePodcast(
            Podcast(
                id = "https://feed.example/rss", title = "Existing", description = "", imageUrl = null, author = null,
                language = null, categories = emptyList(), feedUrl = "https://feed.example/rss", siteUrl = null,
                lastUpdated = 0, isSubscribed = true
            )
        )
        val viewModel = createViewModel()

        viewModel.onUrlChanged("https://feed.example/rss")
        viewModel.addPodcast()

        assertEquals(Res.string.error_podcast_exists, viewModel.uiState.value.error)
    }
}
