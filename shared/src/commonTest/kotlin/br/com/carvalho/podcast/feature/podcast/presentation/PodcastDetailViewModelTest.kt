package br.com.carvalho.podcast.feature.podcast.presentation

import app.cash.turbine.test
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.data.remote.FakeRssFeedDataSource
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
class PodcastDetailViewModelTest {
    private val audioPlayer = FakeAudioPlayer()
    private val repository = FakePodcastRepository()
    private val rssDataSource = FakeRssFeedDataSource()
    private val refreshUseCase = RefreshPodcastUseCase(rssDataSource, repository)
    private val episodeDownloader = FakeEpisodeDownloader()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher)

    private val podcastId = "p1"
    private val samplePodcast = Podcast(id = podcastId, title = "P1", description = "", imageUrl = null, author = null, language = null, categories = emptyList(), feedUrl = podcastId, siteUrl = null, lastUpdated = 0, isSubscribed = true)
    private val sampleEpisode = Episode(id = "e1", podcastId = podcastId, title = "E1", description = null, audioUrl = "", imageUrl = null, duration = 100, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PodcastDetailViewModel(podcastId, audioPlayer, refreshUseCase, episodeDownloader, repository, dispatchers)

    @Test
    fun `initial state loads podcast and episodes`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        repository.episodes.value = listOf(sampleEpisode)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(samplePodcast.id, state.podcast?.id)
            assertEquals(1, state.episodes.size)
            assertEquals(sampleEpisode.id, state.episodes[0].id)
        }
    }

    @Test
    fun `refresh calls use case`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        rssDataSource.feedResult = Result.success(br.com.carvalho.podcast.data.remote.model.RssFeed(
            title = "P1", description = "", imageUrl = null, author = null, language = null, categories = emptyList(), link = null, ttl = null, episodes = emptyList()
        ))
        rssDataSource.delayMs = 10

        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem() // Initial load state

            viewModel.refresh()
            
            val refreshingState = awaitItem()
            assertTrue(refreshingState.isLoading)
            
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            
            assertEquals(podcastId, rssDataSource.fetchFeedCalledWith)
        }
    }

    @Test
    fun `playEpisode prepares and plays via audioPlayer`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        repository.episodes.value = listOf(sampleEpisode)
        
        val viewModel = createViewModel()
        
        viewModel.playEpisode(sampleEpisode)
        
        assertEquals(listOf(sampleEpisode), audioPlayer.queueSet)
        assertEquals(sampleEpisode.id, audioPlayer.playCalledWith?.id)
    }
}
