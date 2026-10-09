package br.com.carvalho.podcast.feature.podcast.presentation

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
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import kotlinx.coroutines.flow.flowOf

/**
 * The screen in each state on a phone, dark theme (17.4). Recorded on Linux by the "Record snapshots" workflow;
 * `verifyRoborazziAndroidHostTest` checks them in CI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class PodcastSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    // The clock stands still, so a spinning loading indicator does not keep the capture waiting for the screen to settle.
    private fun snap(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { PodcastTheme(darkTheme = true) { content() } }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        compose.onRoot().captureRoboImage("snapshots/$PREFIX-$name.png")
    }

    private val episodes = listOf(
        episode("e1", "Como funciona o Pix por dentro"),
        episode("e2", "Carreira em dados", positionMs = 900_000),
        episode("e3", "O que é um banco de dados vetorial", isPlayed = true),
    )

    @Test
    fun loading() = snap("loading") { content(PodcastDetailUiState(isLoading = true), emptyList()) }

    @Test
    fun content() = snap("content") {
        content(
            PodcastDetailUiState(podcast = podcast("p", "Hipsters Ponto Tech"), episodeCount = 3, latestUnplayed = episodes[0]),
            episodes,
        )
    }

    @Composable
    private fun content(state: PodcastDetailUiState, items: List<Episode>) = PodcastDetailContent(
        state = state,
        episodes = flowOf(PagingData.from(items)).collectAsLazyPagingItems(),
        playerState = PlayerState(),
        activeDownloads = emptyMap(),
        actions = PodcastDetailActions(),
    )

    private fun episode(id: String, title: String, positionMs: Long = 0, isPlayed: Boolean = false) = Episode(
        id = id, podcastId = "p", podcastTitle = "Hipsters Ponto Tech", title = title,
        description = "Uma conversa sobre como o sistema funciona por dentro.", audioUrl = "", imageUrl = null,
        duration = 3600, publishDate = 0, isPlayed = isPlayed, playbackPosition = positionMs, isDownloaded = false,
        fileSize = null,
    )

    private fun podcast(id: String, title: String) = Podcast(
        id = id, title = title, description = "Discussões sobre tecnologia, programação e carreira.", imageUrl = null,
        author = "Alura", language = "pt", categories = emptyList(), feedUrl = id, siteUrl = null, lastUpdated = 0,
        isSubscribed = true,
    )

    private companion object {
        const val PREFIX = "podcast"
        const val SETTLE_MS = 500L
    }
}
