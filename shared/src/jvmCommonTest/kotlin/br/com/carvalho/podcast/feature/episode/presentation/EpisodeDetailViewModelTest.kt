package br.com.carvalho.podcast.feature.episode.presentation

import app.cash.turbine.test
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeDetailViewModelTest {
    private val repository = mockk<PodcastRepository>()
    private val audioPlayer = mockk<AudioPlayer>(relaxed = true)
    private val episodeId = "e1"

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        every { audioPlayer.playerState } returns MutableStateFlow(PlayerState())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads episode detail on init`() = runTest {
        val episode = mockk<Episode>(relaxed = true)
        coEvery { repository.getEpisodeById(episodeId) } returns episode

        val viewModel = EpisodeDetailViewModel(episodeId, repository, audioPlayer)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(episode, state.episode)
        }
    }

    @Test
    fun `play calls audioPlayer`() = runTest {
        val episode = mockk<Episode>(relaxed = true)
        coEvery { repository.getEpisodeById(episodeId) } returns episode
        coEvery { audioPlayer.play(any()) } returns Unit

        val viewModel = EpisodeDetailViewModel(episodeId, repository, audioPlayer)

        viewModel.uiState.test {
            awaitItem() // skip initial load state
            viewModel.playEpisode()
            
            // Significant delay to ensure coroutines complete
            kotlinx.coroutines.delay(500)
            
            coVerify(timeout = 1000) { audioPlayer.play(any()) }
        }
    }
}
