package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
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

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val audioPlayer = mockk<AudioPlayer>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)
    private val episodeDownloader = mockk<EpisodeDownloader>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { audioPlayer.playerState } returns MutableStateFlow(PlayerState())
        every { audioPlayer.isReady } returns MutableStateFlow(true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `play calls audioPlayer`() = runTest(testDispatcher) {
        val viewModel = PlayerViewModel(audioPlayer, playerRepository, podcastRepository, episodeDownloader, dispatchers)
        val episode = Episode(id = "e1", podcastId = "p1", title = "E1", description = null, audioUrl = "", imageUrl = null, duration = 100, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null)
        coEvery { episodeDownloader.getLocalPath(any()) } returns null

        viewModel.play(episode)

        coVerify { audioPlayer.play(any()) }
    }
}
