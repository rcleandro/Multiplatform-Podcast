package br.com.carvalho.podcast.feature.episode.presentation

import br.com.carvalho.podcast.core.observability.FakeAnalytics
import app.cash.turbine.test
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
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeDetailViewModelTest {
    private val repository = FakePodcastRepository()
    private val audioPlayer = FakeAudioPlayer()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher)
    private val episodeId = "e1"

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = EpisodeDetailViewModel(
        episodeId, repository, PlayEpisodeUseCase(audioPlayer, repository), dispatchers,
        FakeAnalytics()
    )

    @Test
    fun `loads episode detail on init`() = runTest(testDispatcher) {
        val episode = createEpisode(episodeId, "Ep 1")
        repository.episodes.value = listOf(episode)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(episode.id, state.episode?.id)
        }
    }

    @Test
    fun `play calls audioPlayer`() = runTest(testDispatcher) {
        val episode = createEpisode(episodeId, "Ep 1")
        repository.episodes.value = listOf(episode)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onIntent(EpisodeDetailIntent.Play)

            assertEquals(episodeId, audioPlayer.playCalledWith?.id)
        }
    }

    @Test
    fun `a failed load shows the error state and retry loads the episode`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(createEpisode(episodeId, "Ep 1"))
        repository.getEpisodeError = IllegalStateException("disk")
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.loadFailed)

        repository.getEpisodeError = null
        viewModel.onIntent(EpisodeDetailIntent.Retry)

        val state = viewModel.uiState.value
        assertFalse(state.loadFailed)
        assertEquals(episodeId, state.episode?.id)
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
}
