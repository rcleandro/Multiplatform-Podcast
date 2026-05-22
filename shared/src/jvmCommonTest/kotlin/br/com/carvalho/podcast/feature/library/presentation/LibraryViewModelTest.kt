package br.com.carvalho.podcast.feature.library.presentation

import app.cash.turbine.test
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private val repository = mockk<PodcastRepository>()
    private val addPodcastUseCase = mockk<AddPodcastFromUrlUseCase>()
    private val refreshPodcastUseCase = mockk<RefreshPodcastUseCase>()
    private val deletePodcastUseCase = mockk<DeletePodcastUseCase>()
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

    private fun createViewModel(): LibraryViewModel {
        coEvery { repository.getPodcasts() } returns flowOf(emptyList())
        return LibraryViewModel(repository, addPodcastUseCase, refreshPodcastUseCase, deletePodcastUseCase, dispatchers)
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
            awaitItem() // skip initial
            viewModel.onAddClicked()
            assertTrue(awaitItem().isAddDialogOpen)
        }
    }

    @Test
    fun `addPodcast calls use case and closes dialog`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { addPodcastUseCase(any()) } coAnswers { 
            kotlinx.coroutines.delay(10)
            Result.success(mockk()) 
        }

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.onAddClicked()
            awaitItem()

            viewModel.onUrlChanged("test-url")
            awaitItem()

            viewModel.addPodcast()
            
            // Now we should definitely see the refreshing state
            val refreshingState = awaitItem()
            assertEquals(true, refreshingState.isRefreshing)
            assertEquals(false, refreshingState.isAddDialogOpen)

            val finalState = awaitItem()
            assertFalse(finalState.isRefreshing)
            assertEquals("", finalState.addUrl)
            
            coVerify { addPodcastUseCase("https://test-url") }
        }
    }

    @Test
    fun `confirmDelete calls delete use case`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val podcast = Podcast(id = "1", title = "P1", description = "", imageUrl = null, author = null, language = null, categories = emptyList(), feedUrl = "", siteUrl = null, lastUpdated = 0, isSubscribed = true)
        coEvery { deletePodcastUseCase(any()) } returns Unit

        viewModel.uiState.test {
            awaitItem() // initial
            
            viewModel.onDeleteClicked(podcast)
            awaitItem()

            viewModel.confirmDelete()
            
            val state = awaitItem()
            assertEquals(null, state.podcastToDelete)
            
            coVerify { deletePodcastUseCase("1") }
        }
    }
}
