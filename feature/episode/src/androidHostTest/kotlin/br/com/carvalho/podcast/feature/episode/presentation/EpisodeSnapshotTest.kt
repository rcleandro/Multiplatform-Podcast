package br.com.carvalho.podcast.feature.episode.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import br.com.carvalho.podcast.domain.model.Episode

/**
 * The screen in each state on a phone, dark theme (17.4). Recorded on Linux by the "Record snapshots" workflow;
 * `verifyRoborazziAndroidHostTest` checks them in CI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EpisodeSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    // The clock stands still, so a spinning loading indicator does not keep the capture waiting for the screen to settle.
    private fun snap(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { PodcastTheme(darkTheme = true) { content() } }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        compose.onRoot().captureRoboImage("snapshots/$PREFIX-$name.png")
    }

    @Test
    fun loading() = snap("loading") { EpisodeDetailContent(EpisodeDetailUiState(isLoading = true), EpisodeDetailActions()) }

    @Test
    fun error() = snap("error") { EpisodeDetailContent(EpisodeDetailUiState(loadFailed = true), EpisodeDetailActions()) }

    @Test
    fun content() = snap("content") {
        EpisodeDetailContent(
            EpisodeDetailUiState(episode = episode("e1", "Como funciona o Pix por dentro", positionMs = 900_000)),
            EpisodeDetailActions(),
        )
    }

    private fun episode(id: String, title: String, positionMs: Long = 0, isPlayed: Boolean = false) = Episode(
        id = id, podcastId = "p", podcastTitle = "Hipsters Ponto Tech", title = title,
        description = "Uma conversa sobre como o sistema funciona por dentro.", audioUrl = "", imageUrl = null,
        duration = 3600, publishDate = 0, isPlayed = isPlayed, playbackPosition = positionMs, isDownloaded = false,
        fileSize = null,
    )

    private companion object {
        const val PREFIX = "episode"
        const val SETTLE_MS = 500L
    }
}
