package br.com.carvalho.podcast.feature.library.presentation

import app.cash.turbine.test
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
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

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LibraryViewModel {
        coEvery { repository.getPodcasts() } returns flowOf(emptyList())
        return LibraryViewModel(repository, addPodcastUseCase, refreshPodcastUseCase, deletePodcastUseCase)
    }

    @Test
    fun `initial state is correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.podcasts.isEmpty())
            assertFalse(state.isAddDialogOpen)
        }
    }

    @Test
    fun `onAddClicked opens dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem() // skip initial
            viewModel.onAddClicked()
            assertTrue(awaitItem().isAddDialogOpen)
        }
    }

    @Test
    fun `addPodcast calls use case and closes dialog`() = runTest {
        val viewModel = createViewModel()
        coEvery { addPodcastUseCase(any()) } returns Result.success(mockk())

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.onAddClicked()
            awaitItem()

            viewModel.onUrlChanged("test-url")
            awaitItem()

            viewModel.addPodcast()
            
            assertEquals(true, awaitItem().isRefreshing)
            val finalState = awaitItem()
            assertFalse(finalState.isRefreshing)
            assertFalse(finalState.isAddDialogOpen)
            assertEquals("", finalState.addUrl)
            
            coVerify { addPodcastUseCase("https://test-url") }
        }
    }

    @Test
    fun `confirmDelete calls delete use case`() = runTest {
        val viewModel = createViewModel()
        val podcast = Podcast(id = "1", title = "P1", description = "", imageUrl = null, author = null, language = null, categories = emptyList(), feedUrl = "", siteUrl = null, lastUpdated = 0, isSubscribed = true)
        coEvery { deletePodcastUseCase(any()) } returns Unit

        viewModel.onDeleteClicked(podcast)
        viewModel.confirmDelete()
        
        // Wait for coroutines to complete
        kotlinx.coroutines.delay(500)
        
        coVerify(timeout = 1000) { deletePodcastUseCase("1") }
        assertEquals(null, viewModel.uiState.value.podcastToDelete)
    }
}
