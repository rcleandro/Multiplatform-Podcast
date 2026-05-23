package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePlayerRepository
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
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
class PlayerViewModelTest {
    private val audioPlayer = FakeAudioPlayer()
    private val playerRepository = FakePlayerRepository()
    private val podcastRepository = FakePodcastRepository()
    private val episodeDownloader = FakeEpisodeDownloader()
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

    @Test
    fun `play calls audioPlayer`() = runTest(testDispatcher) {
        val viewModel = PlayerViewModel(audioPlayer, playerRepository, podcastRepository, episodeDownloader, dispatchers)
        val episode = Episode(id = "e1", podcastId = "p1", title = "E1", description = null, audioUrl = "", imageUrl = null, duration = 100, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null)

        viewModel.play(episode)

        assertEquals("e1", audioPlayer.playCalledWith?.id)
    }
}
