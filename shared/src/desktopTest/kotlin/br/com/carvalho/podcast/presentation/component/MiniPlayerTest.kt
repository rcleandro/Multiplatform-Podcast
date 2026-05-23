package br.com.carvalho.podcast.presentation.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.domain.model.Episode
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MiniPlayerTest {

    @Test
    fun miniPlayerDisplaysEpisodeTitleAndHandlesPlayPause() = runComposeUiTest {
        val episode = Episode(
            id = "e1",
            podcastId = "p1",
            title = "Teste Episodio",
            description = null,
            audioUrl = "",
            imageUrl = null,
            duration = 100,
            publishDate = 0,
            isPlayed = false,
            playbackPosition = 0,
            isDownloaded = false,
            fileSize = null
        )

        var playPauseClicked = false

        setContent {
            MiniPlayer(
                episode = episode,
                isPlaying = false,
                isBuffering = false,
                progress = 0f,
                onPlayPauseClick = { playPauseClicked = true },
                onClick = {}
            )
        }

        // Check title
        onNodeWithText("Teste Episodio").assertExists()

        // Check play icon
        onNodeWithContentDescription("Reproduzir").assertExists()

        // Test play click
        onNodeWithContentDescription("Reproduzir").performClick()
        assertEquals(true, playPauseClicked)
    }

    @Test
    fun miniPlayerShowsPauseIconWhenPlaying() = runComposeUiTest {
        val episode = Episode(
            id = "e1",
            podcastId = "p1",
            title = "Teste Episodio",
            description = null,
            audioUrl = "",
            imageUrl = null,
            duration = 100,
            publishDate = 0,
            isPlayed = false,
            playbackPosition = 0,
            isDownloaded = false,
            fileSize = null
        )

        setContent {
            MiniPlayer(
                episode = episode,
                isPlaying = true,
                isBuffering = false,
                progress = 0f,
                onPlayPauseClick = {},
                onClick = {}
            )
        }

        onNodeWithContentDescription("Pausar").assertExists()
    }
}
