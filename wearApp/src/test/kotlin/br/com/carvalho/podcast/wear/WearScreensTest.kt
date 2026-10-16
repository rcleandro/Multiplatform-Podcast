package br.com.carvalho.podcast.wear

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import br.com.carvalho.podcast.core.designsystem.DarkColorScheme
import br.com.carvalho.podcast.core.designsystem.onestFontFamily
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.audio_output
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.subscribed
import br.com.carvalho.podcast.core.ui.generated.resources.volume
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

@OptIn(ExperimentalTestApi::class)
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
        compose.setContent { WearTheme { AppScaffold { content() } } }

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
        watch { NowPlayingContent(PlayerState(currentEpisode = episode, isPlaying = true), onIntent = { intents += it }) }

        compose.onNodeWithContentDescription(text(Res.string.pause)).performClick()

        assertEquals(listOf<PlayerIntent>(PlayerIntent.PlayPause), intents)
    }

    @Test
    fun aPopularPodcastSaysWhenItIsSubscribed() {
        val found = DirectoryPodcast("NerdCast", null, null, "https://nerd.cast/rss", null, null)
        watch { PopularContent(listOf(DirectoryResult(found, isSubscribed = true)), onSubscribe = {}) }

        compose.onNodeWithText(text(Res.string.subscribed)).assertExists()
    }

    @Test
    fun aPodcastWhoseIdIsAFeedAddressOpensWithItsWholeId() {
        val feed = "https://www.omnycontent.com/d/playlist/651a/podcast.rss?key=a%20b"
        var opened: String? = null
        lateinit var navigate: (String) -> Unit
        watch {
            val navController = rememberSwipeDismissableNavController()
            navigate = { navController.navigate(podcastRoute(it)) }
            SwipeDismissableNavHost(navController = navController, startDestination = "home") {
                composable("home") {}
                composable(PODCAST_ROUTE) { opened = it.arguments?.getString(PODCAST_ID) }
            }
        }

        compose.runOnIdle { navigate(feed) }
        compose.waitForIdle()

        assertEquals(feed, opened)
    }

    @Test
    fun theWatchWearsTheBrandsAmberOnBlackInOnest() {
        var primary: Color? = null
        var background: Color? = null
        var family: FontFamily? = null
        var onest: FontFamily? = null
        watch {
            primary = MaterialTheme.colorScheme.primary
            background = MaterialTheme.colorScheme.background
            family = MaterialTheme.typography.bodyLarge.fontFamily
            onest = onestFontFamily()
        }

        assertEquals(DarkColorScheme.primary, primary)
        assertEquals(Color.Black, background)
        assertEquals(onest, family)
    }

    @Test
    fun thePlayerShowsWhereTheEpisodeIsAndHandsVolumeAndOutputToTheWatch() {
        val asked = mutableListOf<String>()
        watch {
            NowPlayingContent(
                PlayerState(currentEpisode = episode.copy(duration = 3_600), position = 754_000, isPlaying = true),
                onIntent = {},
                actions = WatchPlayerActions(
                    onVolumeStep = { asked += if (it) "up" else "down" },
                    onVolume = { asked += "volume" },
                    onAudioOutput = { asked += "output" },
                ),
            )
        }

        compose.onNodeWithText("12:34 / 1:00:00").assertExists()
        compose.onNodeWithContentDescription(text(Res.string.volume)).performClick()
        compose.onNodeWithContentDescription(text(Res.string.audio_output)).performClick()
        // A bezel click is one large event and one step; a crown's small events add up to a step.
        compose.onRoot().performRotaryScrollInput { rotateToScrollVertically(160f) }
        compose.onRoot().performRotaryScrollInput {
            repeat(4) {
                rotateToScrollVertically(-15f)
                advanceEventTime(16)
            }
        }

        assertEquals(listOf("volume", "output", "up", "down"), asked)
    }

    @Test
    fun theProgressRingMovesWithThePlayer() {
        var state by mutableStateOf(PlayerState(currentEpisode = episode.copy(duration = 100), position = 10_000))
        watch { NowPlayingContent(state, onIntent = {}) }
        fun ring() = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

        assertEquals(0.1f, ring(), 0.001f)
        state = state.copy(position = 50_000)
        compose.waitForIdle()

        assertEquals(0.5f, ring(), 0.001f)
    }
}
