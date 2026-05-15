package br.com.carvalho.podcast.feature.player.presentation

import app.cash.turbine.test
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerViewModelTest {
    private val audioPlayer = mockk<AudioPlayer>()
    private val playerRepository = mockk<PlayerRepository>()
    private val podcastRepository = mockk<PodcastRepository>()
    private val episodeDownloader = mockk<EpisodeDownloader>()
    private val playerStateFlow = MutableStateFlow(PlayerState())

    init {
        every { audioPlayer.playerState } returns playerStateFlow
        every { audioPlayer.isReady } returns MutableStateFlow(false)
        every { audioPlayer.release() } just Runs
        coEvery { playerRepository.getSavedPlaybackState() } returns null
        coEvery { episodeDownloader.getLocalPath(any()) } returns null
    }

    @Test
    fun `emits playing state after play is called`() = runTest {
        val viewModel = PlayerViewModel(
            audioPlayer,
            playerRepository,
            podcastRepository,
            episodeDownloader
        )
        val episode = Episode(
            id = "ep1",
            podcastId = "p1",
            title = "Episode 1",
            description = null,
            audioUrl = "",
            imageUrl = null,
            duration = 100,
            publishDate = 0L,
            isPlayed = false,
            playbackPosition = 0,
            isDownloaded = false,
            fileSize = null
        )

        coEvery { audioPlayer.play(any()) } coAnswers {
            playerStateFlow.value = playerStateFlow.value.copy(currentEpisode = episode, isPlaying = true)
        }

        viewModel.playerState.test {
            assertEquals(null, awaitItem().currentEpisode)

            viewModel.play(episode)

            val state = awaitItem()
            assertTrue(state.isPlaying)
            assertEquals("ep1", state.currentEpisode?.id)
        }
    }
}
