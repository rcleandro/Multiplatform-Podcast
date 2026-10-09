package br.com.carvalho.podcast.feature.discover.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * "Add podcast" in each state on a phone, dark theme (18.1). Recorded on Linux by the "Record snapshots" workflow;
 * `verifyRoborazziAndroidHostTest` checks them in CI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DiscoverSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    // The clock stands still, so a spinning loading indicator does not keep the capture waiting for the screen to settle.
    private fun snap(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { PodcastTheme(darkTheme = true) { content() } }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        compose.onRoot().captureRoboImage("snapshots/discover-$name.png")
    }

    private val results = listOf(
        result("Hipsters Ponto Tech", "Alura - Hipsters Network", "Tecnologia", 629, subscribed = true),
        result("IA Sob Controle - Inteligência Artificial", "Alura - Hipsters Network", "Tecnologia", 293),
        result("NerdCast", "Jovem Nerd", "Comédia", 1012),
    )

    @Test
    fun hint() = snap("hint") { DiscoverContent(DiscoverUiState(), DiscoverActions()) }

    @Test
    fun results() = snap("results") { DiscoverContent(DiscoverUiState(query = "hipsters", results = results), DiscoverActions()) }

    @Test
    fun category() = snap("category") {
        DiscoverContent(DiscoverUiState(category = PodcastCategory.TECHNOLOGY, results = results), DiscoverActions())
    }

    @Test
    fun error() = snap("error") { DiscoverContent(DiscoverUiState(query = "news", searchFailed = true), DiscoverActions()) }

    private fun result(title: String, author: String, genre: String, episodes: Int, subscribed: Boolean = false) =
        DirectoryResult(
            DirectoryPodcast(title, author, artworkUrl = null, feedUrl = "https://feeds.example.com/$title", genre, episodes),
            isSubscribed = subscribed,
        )

    private companion object {
        const val SETTLE_MS = 500L
    }
}
