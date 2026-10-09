package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QueueViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val audioPlayer = FakeAudioPlayer()
    private val repository = FakePodcastRepository()
    private val downloader = FakeEpisodeDownloader()

    private val playing = episode("playing")
    private val first = episode("first")
    private val second = episode("second")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = QueueViewModel(
        audioPlayer,
        PlayEpisodeUseCase(audioPlayer),
        repository,
        downloader,
        CoroutineDispatchers(main = dispatcher, io = dispatcher, default = dispatcher),
    ).also { vm -> backgroundScope.launch { vm.uiState.collect {} } }

    @Test
    fun theQueueShowsEachEpisodeAsTheLibraryHasItNow() = runTest(dispatcher) {
        // Queued before it was started and downloaded.
        repository.episodes.value = listOf(first.copy(playbackPosition = 60_000, isDownloaded = true))
        audioPlayer.play(playing)
        audioPlayer.setQueue(listOf(first, second))

        val state = viewModel().uiState.first { it.upNext.isNotEmpty() }

        assertEquals("playing", state.current?.id)
        assertEquals(listOf("first", "second"), state.upNext.map { it.id })
        assertEquals(60_000L, state.upNext[0].playbackPosition)
        assertTrue(state.upNext[0].isDownloaded)
    }

    @Test
    fun aRemovedEpisodeComesBackWhereItWasOnUndo() = runTest(dispatcher) {
        audioPlayer.setQueue(listOf(first, second))
        val viewModel = viewModel()

        viewModel.onIntent(QueueIntent.Remove(first))
        assertEquals(listOf("second"), audioPlayer.playerState.value.queue.map { it.id })

        viewModel.onIntent(QueueIntent.Restore(first, index = 0))
        assertEquals(listOf("first", "second"), audioPlayer.playerState.value.queue.map { it.id })
    }

    @Test
    fun clearingAsksFirstAndEmptiesWhatComesNext() = runTest(dispatcher) {
        audioPlayer.play(playing)
        audioPlayer.setQueue(listOf(first, second))
        val viewModel = viewModel()

        viewModel.onIntent(QueueIntent.RequestClear)
        assertTrue(viewModel.uiState.first { it.isConfirmingClear }.isConfirmingClear)
        assertEquals(2, audioPlayer.playerState.value.queue.size)

        viewModel.onIntent(QueueIntent.ConfirmClear)

        assertEquals(emptyList(), audioPlayer.playerState.value.queue)
        assertEquals("playing", audioPlayer.playerState.value.currentEpisode?.id)
        assertFalse(viewModel.uiState.value.isConfirmingClear)
    }

    @Test
    fun tappingAnEpisodeOfTheQueuePlaysIt() = runTest(dispatcher) {
        audioPlayer.setQueue(listOf(first, second))
        val viewModel = viewModel()

        viewModel.onIntent(QueueIntent.Play(second))

        assertEquals("second", audioPlayer.playCalledWith?.id)
        assertEquals(listOf("first"), audioPlayer.playerState.value.queue.map { it.id })
    }

    @Test
    fun whatIsLeftCountsWholeEpisodesAndTheRestOfStartedOnes() {
        val left = remainingSeconds(
            listOf(
                first.copy(duration = 600),
                second.copy(duration = 600, playbackPosition = 240_000),
                episode("heard").copy(duration = 600, isPlayed = true),
            )
        )

        assertEquals(960L, left)
    }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p", podcastTitle = "Podcast", title = "Episode $id", description = null,
        audioUrl = "https://cdn.example.com/$id.mp3", imageUrl = null, duration = 1800, publishDate = 0,
        isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )
}
