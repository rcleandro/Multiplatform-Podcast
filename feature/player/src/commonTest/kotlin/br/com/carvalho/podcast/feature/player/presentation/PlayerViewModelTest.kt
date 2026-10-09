package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.core.AppConfig
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    private val analytics = FakeAnalytics()

    private fun createViewModel() = PlayerViewModel(
        audioPlayer, PlayEpisodeUseCase(audioPlayer, FakePodcastRepository()), dispatchers, analytics
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

    @Test
    fun `play and pause toggles on the current state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        audioPlayer.play(episode("e1"))

        viewModel.onIntent(PlayerIntent.PlayPause)
        assertTrue(audioPlayer.pauseCalled)

        viewModel.onIntent(PlayerIntent.PlayPause)
        assertTrue(audioPlayer.resumeCalled)
    }

    @Test
    fun `seeking and skipping move the position`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(PlayerIntent.SeekTo(POSITION_MS))
        assertEquals(POSITION_MS, audioPlayer.seekToCalledWith)

        viewModel.onIntent(PlayerIntent.SkipForward)
        assertEquals(POSITION_MS + AppConfig.SKIP_FORWARD_SECONDS * AppConfig.MILLIS_PER_SECOND, position())

        viewModel.onIntent(PlayerIntent.SkipBackward)
        viewModel.onIntent(PlayerIntent.SkipBackward)
        assertEquals(
            POSITION_MS + (AppConfig.SKIP_FORWARD_SECONDS - 2 * AppConfig.SKIP_BACKWARD_SECONDS) * AppConfig.MILLIS_PER_SECOND,
            position(),
        )
    }

    @Test
    fun `next and previous go to the player`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(PlayerIntent.Next)
        viewModel.onIntent(PlayerIntent.Previous)

        assertTrue(audioPlayer.nextCalled)
        assertTrue(audioPlayer.previousCalled)
    }

    @Test
    fun `the chosen speed goes to the player`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(PlayerIntent.SetSpeed(FAST))

        assertEquals(FAST, audioPlayer.playerState.value.speed)
    }

    @Test
    fun `a timer in minutes can be set and cancelled`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(PlayerIntent.SetSleepTimer(SleepTimer.Minutes(MINUTES)))
        assertEquals(SleepTimer.Minutes(MINUTES), audioPlayer.playerState.value.sleepTimer)

        viewModel.onIntent(PlayerIntent.SetSleepTimer(null))
        assertNull(audioPlayer.playerState.value.sleepTimer)
    }

    @Test
    fun `every command is reported to analytics`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        audioPlayer.play(episode("e1"))

        listOf(
            PlayerIntent.PlayPause, PlayerIntent.PlayPause, PlayerIntent.SkipForward, PlayerIntent.Next,
            PlayerIntent.SetSpeed(FAST), PlayerIntent.SetSleepTimer(SleepTimer.EndOfEpisode),
        ).forEach(viewModel::onIntent)

        assertEquals(
            listOf("pause_episode", "resume_episode", "skip_forward", "play_next", "set_speed", "set_sleep_timer"),
            analytics.events,
        )
    }

    private fun position() = audioPlayer.playerState.value.position

        private fun episode(id: String) = Episode(
        id = id, podcastId = "p1", title = id, description = null, audioUrl = "", imageUrl = null, duration = 100,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null
    )

    private companion object {
        const val POSITION_MS = 120_000L
        const val FAST = 1.5f
        const val MINUTES = 15
    }
}
