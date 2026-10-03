package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
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

    private fun createViewModel() = PlayerViewModel(
        audioPlayer, PlayEpisodeUseCase(audioPlayer, FakePodcastRepository()), dispatchers, FakeAnalytics()
    )

    @Test
    fun `picking an episode in the queue plays it and keeps the queue`() = runTest(testDispatcher) {
        val queue = listOf(episode("e1"), episode("e2"))
        audioPlayer.setQueue(queue)
        val viewModel = createViewModel()

        viewModel.onIntent(PlayerIntent.Play(queue[1]))

        assertEquals("e2", audioPlayer.playCalledWith?.id)
        assertEquals(queue, audioPlayer.queueSet)
    }

    @Test
    fun `the sleep timer choice goes to the player`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(PlayerIntent.SetSleepTimer(SleepTimer.EndOfEpisode))

        assertEquals(SleepTimer.EndOfEpisode, audioPlayer.playerState.value.sleepTimer)
    }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p1", title = id, description = null, audioUrl = "", imageUrl = null, duration = 100,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null
    )
}
