package br.com.carvalho.podcast.feature.search.presentation

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
class SearchViewModelTest {
    private val repository = FakePodcastRepository()
    private val episodeDownloader = FakeEpisodeDownloader()
    private val audioPlayer = FakeAudioPlayer()
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

    private fun createViewModel() = SearchViewModel(repository, episodeDownloader, audioPlayer, dispatchers)

    @Test
    fun `initial state is correct`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertEquals(null, state.error)
        }
    }

    @Test
    fun `onQueryChange updates state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onQueryChange("Title")
            
            val state = awaitItem()
            assertEquals("Title", state.searchQuery)
        }
    }

    @Test
    fun `playEpisode calls audioPlayer`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val episode = createEpisode("1", "Title 1")

        viewModel.playEpisode(episode)

        assertEquals("1", audioPlayer.playCalledWith?.id)
    }

    @Test
    fun `downloadEpisode calls downloader`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val episode = createEpisode("1", "Title 1")

        viewModel.downloadEpisode(episode)

        assertEquals("1", episodeDownloader.downloadCalledWith?.id)
    }

    @Test
    fun `deleteDownload calls downloader`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.deleteDownload("1")

        assertEquals("1", episodeDownloader.deleteCalledWith)
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
