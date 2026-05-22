package br.com.carvalho.podcast.feature.search.presentation

import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val repository = mockk<PodcastRepository>()
    private val episodeDownloader = mockk<EpisodeDownloader>()
    private val audioPlayer = mockk<AudioPlayer>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { episodeDownloader.activeDownloads } returns MutableStateFlow(emptyMap())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onQueryChange updates searchQuery state`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(repository, episodeDownloader, audioPlayer)
        viewModel.onQueryChange("kotlin")
        assertEquals("kotlin", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `downloadEpisode calls downloader`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(repository, episodeDownloader, audioPlayer)
        val episode = mockk<Episode>(relaxed = true)
        coEvery { episodeDownloader.download(any()) } returns Unit

        viewModel.downloadEpisode(episode)

        coVerify { episodeDownloader.download(episode) }
    }

    @Test
    fun `deleteDownload calls downloader`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(repository, episodeDownloader, audioPlayer)
        coEvery { episodeDownloader.delete(any()) } returns Unit

        viewModel.deleteDownload("e1")

        coVerify { episodeDownloader.delete("e1") }
    }
}
