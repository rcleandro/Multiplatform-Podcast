package br.com.carvalho.podcast.wear

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.subscribed
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import br.com.carvalho.podcast.feature.player.presentation.PlayerIntent
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// A bare application and a round watch: the real application starts Koin and the background refresh.
@Config(sdk = [35], application = Application::class, qualifiers = "w227dp-h227dp-small-notlong-round-watch-xhdpi")
class WearScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private fun text(res: StringResource) = runBlocking { getString(res) }

    private val podcast = Podcast(
        id = "p1", title = "Hipsters", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = "https://hipsters.tech/feed", siteUrl = null, lastUpdated = 0,
        isSubscribed = true,
    )
    private val episode = Episode(
        id = "e1", podcastId = "p1", title = "Como funciona o Pix", description = null, audioUrl = "", imageUrl = null,
        duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    private fun watch(content: @Composable () -> Unit) =
        compose.setContent { MaterialTheme { AppScaffold { content() } } }

    @Test
    fun theHomeOpensWhatPlaysAndEachPodcast() {
        val opened = mutableListOf<String>()
        watch {
            HomeContent(
                podcasts = listOf(LibraryEntry(podcast, unplayedCount = 3, latestEpisodeDate = null)),
                playing = episode,
                onPlayer = { opened += "player" },
                onPopular = { opened += "popular" },
                onPodcast = { opened += it },
            )
        }

        compose.onNodeWithText("Como funciona o Pix").performClick()
        compose.onNodeWithText("Hipsters").performClick()

        assertEquals(listOf("player", "p1"), opened)
    }

    @Test
    fun thePlayerPausesWhatPlays() {
        val intents = mutableListOf<PlayerIntent>()
        watch { NowPlayingContent(PlayerState(currentEpisode = episode, isPlaying = true)) { intents += it } }

        compose.onNodeWithContentDescription(text(Res.string.pause)).performClick()

        assertEquals(listOf<PlayerIntent>(PlayerIntent.PlayPause), intents)
    }

    @Test
    fun aPopularPodcastSaysWhenItIsSubscribed() {
        val found = DirectoryPodcast("NerdCast", null, null, "https://nerd.cast/rss", null, null)
        watch { PopularContent(listOf(DirectoryResult(found, isSubscribed = true)), onSubscribe = {}) }

        compose.onNodeWithText(text(Res.string.subscribed)).assertExists()
    }
}
