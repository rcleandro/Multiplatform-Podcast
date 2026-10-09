package br.com.carvalho.podcast.feature.library.presentation

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
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.Podcast

/**
 * The screen in each state on a phone, dark theme (17.4). Recorded on Linux by the "Record snapshots" workflow;
 * `verifyRoborazziAndroidHostTest` checks them in CI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LibrarySnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    // The clock stands still, so a spinning loading indicator does not keep the capture waiting for the screen to settle.
    private fun snap(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { PodcastTheme(darkTheme = true) { content() } }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        compose.onRoot().captureRoboImage("snapshots/$PREFIX-$name.png")
    }

    private val entries = listOf(
        LibraryEntry(podcast("a", "Hipsters Ponto Tech"), unplayedCount = 3, latestEpisodeDate = null),
        LibraryEntry(podcast("b", "NerdCast"), unplayedCount = 0, latestEpisodeDate = null),
        LibraryEntry(podcast("c", "The Daily"), unplayedCount = 12, latestEpisodeDate = null),
    )
    private val started = listOf(episode("e1", "Como funciona o Pix por dentro", positionMs = 1_200_000))

    @Test
    fun empty() = snap("empty") { LibraryContent(LibraryUiState(), LibraryActions()) }

    @Test
    fun loading() = snap("loading") { LibraryContent(LibraryUiState(isLoading = true), LibraryActions()) }

    @Test
    fun grid() = snap("grid") { LibraryContent(LibraryUiState(podcasts = entries, inProgress = started), LibraryActions()) }

    @Test
    fun list() = snap("list") {
        LibraryContent(LibraryUiState(podcasts = entries, layout = LibraryLayout.LIST), LibraryActions())
    }

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
        const val PREFIX = "library"
        const val SETTLE_MS = 500L
    }
}
