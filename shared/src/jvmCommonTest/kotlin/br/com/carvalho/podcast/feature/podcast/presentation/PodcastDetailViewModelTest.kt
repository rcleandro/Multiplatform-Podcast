package br.com.carvalho.podcast.feature.podcast.presentation

import app.cash.turbine.test
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class PodcastDetailViewModelTest {
    private val audioPlayer = mockk<AudioPlayer>()
    private val refreshUseCase = mockk<RefreshPodcastUseCase>()
    private val episodeDownloader = mockk<EpisodeDownloader>()
    private val repository = mockk<PodcastRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val podcastId = "p1"
    private val samplePodcast = Podcast(id = podcastId, title = "P1", description = "", imageUrl = null, author = null, language = null, categories = emptyList(), feedUrl = "", siteUrl = null, lastUpdated = 0, isSubscribed = true)
    private val sampleEpisode = Episode(id = "e1", podcastId = podcastId, title = "E1", description = null, audioUrl = "", imageUrl = null, duration = 100, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { audioPlayer.playerState } returns MutableStateFlow(PlayerState())
        every { episodeDownloader.activeDownloads } returns MutableStateFlow(emptyMap())
        coEvery { repository.getEpisodesPaged(any()) } returns flowOf(mockk())
        coEvery { repository.getPodcastByIdFlow(any()) } returns flowOf(samplePodcast)
        coEvery { repository.getEpisodes(any()) } returns flowOf(listOf(sampleEpisode))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PodcastDetailViewModel(podcastId, audioPlayer, refreshUseCase, episodeDownloader, repository)

    @Test
    fun `initial state loads podcast and episodes`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(samplePodcast, state.podcast)
            assertEquals(listOf(sampleEpisode), state.episodes)
        }
    }

    @Test
    fun `refresh calls use case`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        // Use a small delay to prevent StateFlow coalescing and ensure we see the refreshing state
        coEvery { refreshUseCase(podcastId) } coAnswers {
            kotlinx.coroutines.delay(1)
            Result.success(Unit)
        }

        viewModel.uiState.test {
            // Initial state (already loaded)
            val initialState = awaitItem()
            assertFalse(initialState.isRefreshing)

            viewModel.refresh()
            
            // We expect to see the refreshing state
            assertTrue(awaitItem().isRefreshing)
            
            // And finally the completed state
            assertFalse(awaitItem().isRefreshing)
            
            coVerify { refreshUseCase(podcastId) }
        }
    }

    @Test
    fun `playEpisode prepares and plays via audioPlayer`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { episodeDownloader.getLocalPath(any()) } returns null
        coEvery { audioPlayer.setQueue(any()) } returns Unit
        coEvery { audioPlayer.play(any()) } returns Unit

        viewModel.uiState.test {
            awaitItem() // Wait for initial load to populate episodes in uiState
            viewModel.playEpisode("e1")
            coVerify { audioPlayer.setQueue(any()) }
            coVerify { audioPlayer.play(any()) }
        }
    }
}
