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
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
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
import br.com.carvalho.podcast.core.ui.generated.resources.cancel_download
import br.com.carvalho.podcast.core.ui.generated.resources.download_cd
import br.com.carvalho.podcast.core.ui.generated.resources.episode_downloading
import br.com.carvalho.podcast.core.ui.generated.resources.episode_played
import br.com.carvalho.podcast.core.ui.generated.resources.episode_playing
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.play_next
import br.com.carvalho.podcast.core.ui.generated.resources.remaining_time
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlists
import br.com.carvalho.podcast.core.ui.generated.resources.subscribed
import br.com.carvalho.podcast.core.ui.generated.resources.subscribing
import br.com.carvalho.podcast.core.ui.generated.resources.volume
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.SmartPlaylist
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailIntent
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailUiState
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

    /** Lists compose only what is near the screen: scroll the item in first. */
    private fun tapAfterScrolling(label: String) {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(label))
        compose.onNodeWithText(label).performClick()
    }

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
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Hipsters"))
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

    @Test
    fun aPodcastBeingSubscribedToSaysSoUntilItIsDone() {
        val found = DirectoryPodcast("NerdCast", null, null, "https://nerd.cast/rss", null, null)
        var subscribing by mutableStateOf(setOf(found.feedUrl))
        watch {
            PopularContent(listOf(DirectoryResult(found, isSubscribed = false)), onSubscribe = {}, subscribing = subscribing)
        }
        compose.onNodeWithText(text(Res.string.subscribing)).assertExists()

        subscribing = emptySet()

        compose.onNodeWithText(text(Res.string.subscribing)).assertDoesNotExist()
    }

    @Test
    fun eachEpisodeSaysWhereItStands() {
        var row by mutableStateOf(Triple(episode, false, null as DownloadStatus?))
        watch { EpisodeButton(row.first, isPlaying = row.second, download = row.third, onPlay = {}, onActions = {}) }
        fun says(res: StringResource) = compose.onNodeWithText(text(res), substring = true).assertExists()

        row = Triple(episode, true, null)
        says(Res.string.episode_playing)
        row = Triple(episode, false, DownloadStatus.Downloading(0.4f, 40, 100))
        says(Res.string.episode_downloading)
        row = Triple(episode.copy(isPlayed = true, isDownloaded = true), false, null)
        says(Res.string.episode_played)
        says(Res.string.filter_downloaded)
        row = Triple(episode.copy(duration = 1_800, playbackPosition = 600_000), false, null)
        compose.onNodeWithText(runBlocking { getString(Res.string.remaining_time, "20min") }).assertExists()
    }

    @Test
    fun theInfoPageShowsTheEpisodeAndOpensSpeedAndTimer() {
        val opened = mutableListOf<String>()
        val playing = episode.copy(podcastTitle = "Hipsters", description = "<p>Sobre <b>Pix</b></p>")
        watch {
            EpisodeInfoPage(
                PlayerState(currentEpisode = playing, speed = 1.5f),
                timerLabel = "4:59",
                onSpeed = { opened += "speed" },
                onTimer = { opened += "timer" },
            )
        }

        compose.onNodeWithText("1.5×").performClick()
        compose.onNodeWithText("4:59").performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Sobre Pix", substring = true))

        assertEquals(listOf("speed", "timer"), opened)
    }

    @Test
    fun aChoiceListChecksTheCurrentChoiceAndReturnsTheTappedOne() {
        var chosen: Int? = null
        watch { ChoiceList("Speed", listOf("1×" to true, "2×" to false), onChoose = { chosen = it }) }

        compose.onNodeWithText("2×").performClick()

        assertEquals(1, chosen)
    }

    @Test
    fun theQueuePlaysATappedEpisodeAndALongPressTakesItOut() {
        val next = episode.copy(id = "e2", title = "Rust no backend")
        val played = mutableListOf<String>()
        val removed = mutableListOf<String>()
        watch {
            QueueContent(
                current = episode,
                isPlaying = true,
                upNext = listOf(next),
                onPlay = { played += it.id },
                onRemove = { removed += it.id },
            )
        }

        compose.onNodeWithText("Rust no backend").performClick()
        compose.onNodeWithText("Rust no backend").performTouchInput { longClick() }

        assertEquals(listOf("e2"), played)
        assertEquals(listOf("e2"), removed)
    }

    @Test
    fun anEpisodesActionsQueueDownloadAndMarkIt() {
        val intents = mutableListOf<EpisodeDetailIntent>()
        var state by mutableStateOf(EpisodeDetailUiState(episode = episode))
        watch { EpisodeActionsContent(state) { intents += it } }

        compose.onNodeWithText(text(Res.string.play_next)).performClick()
        tapAfterScrolling(text(Res.string.download_cd))
        tapAfterScrolling(text(Res.string.mark_as_played))
        state = state.copy(downloadStatus = DownloadStatus.Downloading(0.5f, 5, 10))
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(Res.string.cancel_download)))

        assertEquals(
            listOf(EpisodeDetailIntent.Enqueue(next = true), EpisodeDetailIntent.Download, EpisodeDetailIntent.MarkPlayed),
            intents,
        )
    }

    @Test
    fun theHomeOpensDownloadsAndPlaylistsAndAPlaylistOpensByItsId() {
        val opened = mutableListOf<String>()
        watch {
            HomeContent(
                podcasts = emptyList(),
                playing = null,
                onPlayer = {},
                onPopular = {},
                onPodcast = {},
                onDownloads = { opened += "downloads" },
                onPlaylists = { opened += "playlists" },
            )
        }
        compose.onNodeWithText(text(Res.string.filter_downloaded)).performClick()
        compose.onNodeWithText(text(Res.string.smart_playlists)).performClick()

        assertEquals(listOf("downloads", "playlists"), opened)
    }

    @Test
    fun aPlaylistOpensByItsId() {
        var opened: Long? = null
        watch { PlaylistsContent(listOf(SmartPlaylist(id = 7, name = "Curtos")), onPlaylist = { opened = it }) }

        compose.onNodeWithText("Curtos").performClick()

        assertEquals(7L, opened)
    }

    @Test
    fun aUsualPlaylistIsMadeWithATapWhenThereIsNoKeyboard() {
        val made = mutableListOf<SmartPlaylist>()
        val preset = SmartPlaylist(name = "Não ouvidos", unplayedOnly = true)
        watch { PlaylistsContent(emptyList(), onPlaylist = {}, presets = listOf(preset), onCreate = { made += it }) }

        compose.onNodeWithText("Não ouvidos").performClick()

        assertEquals(listOf(preset), made)
    }
}
