package br.com.carvalho.podcast.feature.downloads.presentation

import app.cash.turbine.test
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
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
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadedEpisodesViewModelTest {
    private val repository = FakePodcastRepository()
    private val episodeDownloader = FakeEpisodeDownloader()
    private val audioPlayer = FakeAudioPlayer()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher)

    private val sampleEpisode = Episode(id = "e1", podcastId = "p1", title = "E1", description = null, audioUrl = "", imageUrl = null, duration = 100, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = true, fileSize = null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DownloadedEpisodesViewModel(repository, episodeDownloader, audioPlayer, dispatchers)

    @Test
    fun `loads downloaded episodes initially`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(sampleEpisode)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertEquals(listOf(sampleEpisode), awaitItem().episodes)
        }
    }

    @Test
    fun `deleteDownload calls downloader and shows snackbar`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial state
            viewModel.deleteDownload("e1")
            
            val state = awaitItem()
            assertEquals("Download excluído", state.snackbarMessage)
            assertEquals("e1", episodeDownloader.deleteCalledWith)
        }
    }

    @Test
    fun `playEpisode prepares player with queue`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.playEpisode(sampleEpisode)

        assertEquals("e1", audioPlayer.playCalledWith?.id)
    }
}
