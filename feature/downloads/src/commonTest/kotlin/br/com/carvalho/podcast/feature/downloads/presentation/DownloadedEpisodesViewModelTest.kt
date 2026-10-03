package br.com.carvalho.podcast.feature.downloads.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.download_deleted
import br.com.carvalho.podcast.core.ui.generated.resources.Res
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
import br.com.carvalho.podcast.presentation.UiMessage
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
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

    private fun createViewModel() = DownloadedEpisodesViewModel(
        repository, episodeDownloader, audioPlayer, PlayEpisodeUseCase(audioPlayer, episodeDownloader, repository), dispatchers
    )

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

        viewModel.onIntent(DownloadsIntent.ConfirmDelete(sampleEpisode))

        viewModel.messages.test {
            assertEquals(UiMessage(Res.string.download_deleted), awaitItem())
        }
        assertEquals("e1", episodeDownloader.deleteCalledWith)
    }

    @Test
    fun `playEpisode prepares player with queue`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(DownloadsIntent.Play(sampleEpisode))

        assertEquals("e1", audioPlayer.playCalledWith?.id)
    }
}
