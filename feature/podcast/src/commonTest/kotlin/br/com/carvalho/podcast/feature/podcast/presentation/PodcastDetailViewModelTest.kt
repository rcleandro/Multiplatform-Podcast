package br.com.carvalho.podcast.feature.podcast.presentation

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_no_connection
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import app.cash.turbine.test
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
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
class PodcastDetailViewModelTest {
    private val audioPlayer = FakeAudioPlayer()
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()
    private val refreshUseCase = RefreshPodcastUseCase(feedSource, repository)
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

    private fun createViewModel() = PodcastDetailViewModel(podcastId, audioPlayer, refreshUseCase, episodeDownloader, repository, dispatchers, FakeAnalytics())

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
        feedSource.result = Result.success(FetchedFeed(samplePodcast, emptyList()))
        feedSource.delayMs = 10

        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(PodcastDetailIntent.Refresh)

            val refreshingState = awaitItem()
            assertTrue(refreshingState.isRefreshing)
            assertFalse(refreshingState.isLoading)

            val finalState = awaitItem()
            assertFalse(finalState.isRefreshing)
            
            assertEquals(podcastId, feedSource.fetchCalledWith)
        }
    }

    @Test
    fun `playEpisode prepares and plays via audioPlayer`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        repository.episodes.value = listOf(sampleEpisode)
        
        val viewModel = createViewModel()
        
        viewModel.onIntent(PodcastDetailIntent.Play(sampleEpisode))
        
        assertEquals(listOf(sampleEpisode), audioPlayer.queueSet)
        assertEquals(sampleEpisode.id, audioPlayer.playCalledWith?.id)
    }

    @Test
    fun `cancelDownload cancels the episode download`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.CancelDownload(sampleEpisode.copy(id = "episode-1")))

        assertEquals("episode-1", episodeDownloader.cancelCalledWith)
    }

    @Test
    fun `filter survives an episode list update`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        repository.episodes.value = listOf(sampleEpisode)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.SetFilter(EpisodeFilter.UNPLAYED))
        repository.episodes.value = listOf(sampleEpisode.copy(isPlayed = true))

        assertEquals(EpisodeFilter.UNPLAYED, viewModel.uiState.value.filter)
    }

    @Test
    fun `a failed refresh tells the user why`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        feedSource.result = Result.failure(AppError.NoConnection)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.Refresh)

        viewModel.messages.test {
            assertEquals(Res.string.error_no_connection, awaitItem())
        }
        assertFalse(viewModel.uiState.value.isRefreshing)
    }
}
