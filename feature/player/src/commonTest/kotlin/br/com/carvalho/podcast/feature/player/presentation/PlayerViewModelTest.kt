package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePlayerRepository
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val audioPlayer = FakeAudioPlayer()
    private val playerRepository = FakePlayerRepository()
    private val podcastRepository = FakePodcastRepository()
    private val episodeDownloader = FakeEpisodeDownloader()
    private val playEpisode = PlayEpisodeUseCase(audioPlayer, episodeDownloader, podcastRepository)
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
        val viewModel = PlayerViewModel(
            audioPlayer, playerRepository, podcastRepository, episodeDownloader, playEpisode, dispatchers, FakeAnalytics()
        )
        val episode = Episode(id = "e1", podcastId = "p1", title = "E1", description = null, audioUrl = "", imageUrl = null, duration = 100, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null)

        viewModel.onIntent(PlayerIntent.Play(episode))

        assertEquals("e1", audioPlayer.playCalledWith?.id)
    }

    @Test
    fun `closing the player screen leaves the shared player working`() = runTest(testDispatcher) {
        val store = ViewModelStore()
        val factory = viewModelFactory {
            initializer {
                PlayerViewModel(
            audioPlayer, playerRepository, podcastRepository, episodeDownloader, playEpisode, dispatchers, FakeAnalytics()
        )
            }
        }
        ViewModelProvider.create(store, factory)[PlayerViewModel::class]

        store.clear()

        assertFalse(audioPlayer.releaseCalled)
    }

    @Test
    fun `progress is saved while the episode keeps playing`() {
        val virtualTime = StandardTestDispatcher()
        Dispatchers.setMain(virtualTime)
        runTest(virtualTime) {
            val timed = CoroutineDispatchers(main = virtualTime, io = virtualTime, default = virtualTime)
            PlayerViewModel(
                audioPlayer, playerRepository, podcastRepository, episodeDownloader, playEpisode, timed, FakeAnalytics()
            )
            audioPlayer.play(episode)
            runCurrent()
            val savesBefore = playerRepository.saveCount

            // One minute of playback, with the position moving every 500 ms as the real players do.
            repeat(TICKS_PER_MINUTE) { tick ->
                advanceTimeBy(TICK_MS)
                audioPlayer.advanceTo((tick + 1) * TICK_MS)
            }
            runCurrent()

            val saves = playerRepository.saveCount - savesBefore
            assertTrue(saves >= MIN_SAVES_PER_MINUTE, "only $saves saves in a minute of playback")
            assertTrue(playerRepository.savedPlaybackState!!.position >= MINUTE_MS - SAVE_LAG_MS)
        }
    }

    private val episode = Episode(
        id = "e1", podcastId = "p1", title = "E1", description = null, audioUrl = "", imageUrl = null, duration = 3600,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null
    )

    private companion object {
        const val TICK_MS = 500L
        const val MINUTE_MS = 60_000L
        const val TICKS_PER_MINUTE = (MINUTE_MS / TICK_MS).toInt()
        /** At most one save interval (5 s) between saves: at least 10 in a minute. */
        const val MIN_SAVES_PER_MINUTE = 10
        const val SAVE_LAG_MS = 5_000L
    }
}
