package br.com.carvalho.podcast.feature.downloads.presentation

import app.cash.turbine.test
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadedEpisodesViewModelTest {
    private val repository = mockk<PodcastRepository>()
    private val episodeDownloader = mockk<EpisodeDownloader>()
    private val audioPlayer = mockk<AudioPlayer>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val sampleEpisode = Episode(id = "e1", podcastId = "p1", title = "E1", description = null, audioUrl = "", imageUrl = null, duration = 100, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = true, fileSize = null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { audioPlayer.playerState } returns MutableStateFlow(PlayerState())
        every { episodeDownloader.activeDownloads } returns MutableStateFlow(emptyMap())
        coEvery { repository.getDownloadedEpisodes() } returns flowOf(listOf(sampleEpisode))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads downloaded episodes initially`() = runTest(testDispatcher) {
        val viewModel = DownloadedEpisodesViewModel(repository, episodeDownloader, audioPlayer)
        viewModel.uiState.test {
            assertEquals(listOf(sampleEpisode), awaitItem().episodes)
        }
    }

    @Test
    fun `deleteDownload calls downloader and shows snackbar`() = runTest(testDispatcher) {
        val viewModel = DownloadedEpisodesViewModel(repository, episodeDownloader, audioPlayer)
        coEvery { episodeDownloader.delete(any()) } returns Unit

        viewModel.deleteDownload("e1")

        coVerify { episodeDownloader.delete("e1") }
        assertEquals("Download excluído", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `playEpisode prepares player with queue`() = runTest(testDispatcher) {
        val viewModel = DownloadedEpisodesViewModel(repository, episodeDownloader, audioPlayer)
        coEvery { episodeDownloader.getLocalPath(any()) } returns "/path"
        coEvery { audioPlayer.setQueue(any()) } returns Unit
        coEvery { audioPlayer.play(any()) } returns Unit

        viewModel.playEpisode(sampleEpisode)

        coVerify { audioPlayer.play(match { it.localPath == "/path" }) }
    }
}
