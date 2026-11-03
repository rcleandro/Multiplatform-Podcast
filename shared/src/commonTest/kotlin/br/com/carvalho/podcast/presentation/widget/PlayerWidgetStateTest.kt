package br.com.carvalho.podcast.presentation.widget

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.usecase.ExplicitRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerWidgetStateTest {
    private val playing = episode("playing")

    @Test
    fun theStartedEpisodesLeaveOutTheOnePlaying() = runTest {
        val state = playerWidgetState(
            flowOf(PlayerState(currentEpisode = playing, isPlaying = true)),
            flowOf(listOf(playing, episode("started"))),
            flowOf(ExplicitRule()),
        ).first()

        assertEquals("playing", state.episode?.id)
        assertTrue(state.isPlaying)
        assertEquals(listOf("started"), state.continueListening.map { it.id })
    }

    @Test
    fun underTheEcaDigitalNothingExplicitIsShown() = runTest {
        val state = playerWidgetState(
            flowOf(PlayerState()),
            flowOf(listOf(episode("clean"), episode("swearing").copy(explicit = true))),
            flowOf(ExplicitRule(hidesExplicit = true)),
        ).first()

        assertEquals(listOf("clean"), state.continueListening.map { it.id })
    }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p", title = id, description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = 0, isPlayed = false, playbackPosition = 1, isDownloaded = false, fileSize = null,
    )
}
