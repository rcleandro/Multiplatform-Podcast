package br.com.carvalho.podcast.feature.search.presentation

import br.com.carvalho.podcast.core.observability.FakeAnalytics
import app.cash.turbine.test
import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_storage_full
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.presentation.UiMessage
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

    private fun createViewModel() = SearchViewModel(
        repository, episodeDownloader, audioPlayer, PlayEpisodeUseCase(audioPlayer, repository),
        dispatchers, FakeAnalytics()
    )

    @Test
    fun `initial state is correct`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
        }
    }

    @Test
    fun `onQueryChange updates state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(SearchIntent.ChangeQuery("Title"))
            
            val state = awaitItem()
            assertEquals("Title", state.searchQuery)
        }
    }

    @Test
    fun `a download that fails tells why, once, and not for failures from before`() = runTest(testDispatcher) {
        val old = DownloadStatus.Failed(AppError.NoConnection)
        episodeDownloader.activeDownloads.value = mapOf("old" to old)
        val viewModel = createViewModel()

        viewModel.messages.test {
            episodeDownloader.activeDownloads.value = mapOf("old" to old, "e1" to DownloadStatus.Downloading(0.5f, 1, 2))
            episodeDownloader.activeDownloads.value =
                mapOf("old" to old, "e1" to DownloadStatus.Failed(AppError.StorageFull))
            assertEquals(UiMessage(Res.string.error_storage_full), awaitItem())
            episodeDownloader.activeDownloads.value =
                mapOf("old" to old, "e1" to DownloadStatus.Failed(AppError.StorageFull), "e2" to DownloadStatus.Idle)
            expectNoEvents()
        }
    }

    @Test
    fun `playEpisode calls audioPlayer`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val episode = createEpisode("1", "Title 1")

        viewModel.onIntent(SearchIntent.Play(episode))

        assertEquals("1", audioPlayer.playCalledWith?.id)
    }

    @Test
    fun `downloadEpisode calls downloader`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val episode = createEpisode("1", "Title 1")

        viewModel.onIntent(SearchIntent.Download(episode))

        assertEquals("1", episodeDownloader.downloadCalledWith?.id)
    }

    @Test
    fun `deleteDownload calls downloader`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.onIntent(SearchIntent.ConfirmDeleteDownload(createEpisode("1", "Title 1")))

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

    @Test
    fun `cancelDownload cancels the episode download`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.CancelDownload(createEpisode("episode-1", "Title")))

        assertEquals("episode-1", episodeDownloader.cancelCalledWith)
    }
}
