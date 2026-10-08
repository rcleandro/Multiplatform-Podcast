package br.com.carvalho.podcast.feature.episode.presentation

import br.com.carvalho.podcast.core.observability.FakeAnalytics
import app.cash.turbine.test
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.download.DownloadStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeDetailViewModelTest {
    private val repository = FakePodcastRepository()
    private val audioPlayer = FakeAudioPlayer()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher)
    private val episodeId = "e1"

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val downloader = FakeEpisodeDownloader()
    private val sampleEpisode = Episode(
        id = episodeId, podcastId = "p", title = "Episode", description = null, audioUrl = "a", imageUrl = null,
        duration = 60, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    private fun createViewModel() = EpisodeDetailViewModel(
        episodeId, repository, PlayEpisodeUseCase(audioPlayer, repository), audioPlayer, downloader, dispatchers,
        FakeAnalytics()
    )

    @Test
    fun `the main button pauses the episode that is playing`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(sampleEpisode)
        val viewModel = createViewModel()
        viewModel.onIntent(EpisodeDetailIntent.PlayPause)
        assertEquals(true, viewModel.uiState.value.isPlaying)

        viewModel.onIntent(EpisodeDetailIntent.PlayPause)

        assertEquals(true, audioPlayer.pauseCalled)
        assertEquals(false, viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `marking played and unplayed shows on the screen`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(sampleEpisode)
        val viewModel = createViewModel()

        viewModel.onIntent(EpisodeDetailIntent.MarkPlayed)
        assertEquals(true, viewModel.uiState.value.episode?.isPlayed)

        viewModel.onIntent(EpisodeDetailIntent.MarkUnplayed)
        assertEquals(false, viewModel.uiState.value.episode?.isPlayed)
    }

    @Test
    fun `the download button follows the download`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(sampleEpisode)
        val viewModel = createViewModel()

        viewModel.onIntent(EpisodeDetailIntent.Download)
        downloader.activeDownloads.value = mapOf(sampleEpisode.id to DownloadStatus.Downloading(0.5f, 50, 100))

        assertEquals(sampleEpisode.id, downloader.downloadCalledWith?.id)
        assertEquals(DownloadStatus.Downloading(0.5f, 50, 100), viewModel.uiState.value.downloadStatus)
    }

    @Test
    fun `loads episode detail on init`() = runTest(testDispatcher) {
        val episode = createEpisode(episodeId, "Ep 1")
        repository.episodes.value = listOf(episode)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(episode.id, state.episode?.id)
        }
    }

    @Test
    fun `play calls audioPlayer`() = runTest(testDispatcher) {
        val episode = createEpisode(episodeId, "Ep 1")
        repository.episodes.value = listOf(episode)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onIntent(EpisodeDetailIntent.PlayPause)

            assertEquals(episodeId, audioPlayer.playCalledWith?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed load shows the error state and retry loads the episode`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(createEpisode(episodeId, "Ep 1"))
        repository.getEpisodeError = IllegalStateException("disk")
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.loadFailed)

        repository.getEpisodeError = null
        viewModel.onIntent(EpisodeDetailIntent.Retry)

        val state = viewModel.uiState.value
        assertFalse(state.loadFailed)
        assertEquals(episodeId, state.episode?.id)
    }

    private fun createEpisode(id: String, title: String) = Episode(
        id = id,
        podcastId = "p1",
        title = title,
        description = null,
        audioUrl = "url",
        imageUrl = null,
        duration = 0L,
        publishDate = 0L,
        isPlayed = false,
        playbackPosition = 0L,
        isDownloaded = false,
        fileSize = null
    )
}
