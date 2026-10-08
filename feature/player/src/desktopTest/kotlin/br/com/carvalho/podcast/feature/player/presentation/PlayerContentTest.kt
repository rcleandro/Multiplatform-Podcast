package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.core.designsystem.Spacing
import kotlin.math.abs
import br.com.carvalho.podcast.core.designsystem.TabletopFold
import br.com.carvalho.podcast.core.designsystem.LocalTabletopFold
import androidx.compose.runtime.CompositionLocalProvider
import kotlin.test.assertTrue
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.next
import br.com.carvalho.podcast.core.ui.generated.resources.previous
import br.com.carvalho.podcast.core.ui.generated.resources.state_on
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.model.Podcast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import br.com.carvalho.podcast.core.designsystem.Motion
import kotlin.test.assertNotEquals
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.play
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class PlayerContentTest {

    private companion object {
        const val WIDE = 1200
        const val PHONE = 400
        const val TALL = 900
        const val SIDEWAYS_WIDTH = 780
        const val SIDEWAYS_HEIGHT = 360
        const val FOLD_TOP = 360

        /** The window an app gets on the Razr 60's cover screen, in dp. */
        const val COVER_WIDTH = 469
        const val COVER_HEIGHT = 318
        const val FOLD_THICKNESS = 20

        /** The aux row under the controls moves them up a bit from the exact center. */
        const val CENTER_TOLERANCE = 60f
    }

    private fun text(res: StringResource) = runBlocking { getString(res) }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p", podcastTitle = "Podcast", title = "Episode $id", description = null,
        audioUrl = "", imageUrl = null, duration = 60, publishDate = 0, isPlayed = false,
        playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    @Test
    fun thePlayerHandsItsCoverModifierToTheCover() = runComposeUiTest {
        // The navigation shares the cover with the mini player through this modifier.
        setContent {
            PodcastTheme {
                PlayerContent(
                    state = PlayerState(currentEpisode = episode("e1")),
                    actions = PlayerActions(),
                    artworkModifier = Modifier.testTag("cover"),
                )
            }
        }
        onNodeWithTag("cover").assertExists()
    }

    @Test
    fun aWideWindowPutsTheCoverBesideTheControls() = runDesktopComposeUiTest(width = WIDE, height = TALL) {
        setContent {
            PodcastTheme {
                PlayerContent(
                    state = PlayerState(currentEpisode = episode("e1"), isPlaying = true),
                    actions = PlayerActions(),
                    artworkModifier = Modifier.testTag("cover"),
                )
            }
        }
        val cover = onNodeWithTag("cover").getBoundsInRoot()
        val play = onNodeWithContentDescription(text(Res.string.pause)).getBoundsInRoot()

        assertTrue(cover.right <= play.left, "cover $cover should be left of the play button $play")
    }

    @Test
    fun aPhoneWindowPutsTheCoverAboveTheControls() = runDesktopComposeUiTest(width = PHONE, height = TALL) {
        setContent {
            PodcastTheme {
                PlayerContent(
                    state = PlayerState(currentEpisode = episode("e1"), isPlaying = true),
                    actions = PlayerActions(),
                    artworkModifier = Modifier.testTag("cover"),
                )
            }
        }
        val cover = onNodeWithTag("cover").getBoundsInRoot()
        val play = onNodeWithContentDescription(text(Res.string.pause)).getBoundsInRoot()

        assertTrue(cover.bottom <= play.top, "cover $cover should be above the play button $play")
    }

    @Test
    fun aPhoneOnItsSideShowsTheWholeCoverAndThePlayButtonWithoutScrolling() =
        runDesktopComposeUiTest(width = SIDEWAYS_WIDTH, height = SIDEWAYS_HEIGHT) {
            setContent {
                PodcastTheme {
                    PlayerContent(
                        state = PlayerState(currentEpisode = episode("e1"), isPlaying = true),
                        actions = PlayerActions(),
                        artworkModifier = Modifier.testTag("cover"),
                    )
                }
            }
            val window = onRoot().getBoundsInRoot()
            val cover = onNodeWithTag("cover").getBoundsInRoot()
            val play = onNodeWithContentDescription(text(Res.string.pause)).getBoundsInRoot()

            assertTrue(cover.right <= play.left, "cover $cover should be left of the play button $play")
            assertTrue(cover.bottom <= window.bottom, "cover $cover should fit in the window $window")
            assertTrue(play.bottom <= window.bottom, "play button $play should be in the window $window")
        }

    @Test
    fun aCoverScreenShowsEveryControlWithoutTheCover() = runDesktopComposeUiTest(width = COVER_WIDTH, height = COVER_HEIGHT) {
        setContent {
            PodcastTheme {
                PlayerContent(
                    state = PlayerState(currentEpisode = episode("e1"), isPlaying = true),
                    actions = PlayerActions(),
                    artworkModifier = Modifier.testTag("cover"),
                )
            }
        }
        val window = onRoot().getBoundsInRoot()

        onNodeWithTag("cover").assertDoesNotExist()
        listOf(Res.string.previous, Res.string.pause, Res.string.next).forEach { control ->
            val bounds = onNodeWithContentDescription(text(control)).getBoundsInRoot()
            assertTrue(bounds.right <= window.right && bounds.bottom <= window.bottom, "$control at $bounds is cut off")
        }
    }

    @Test
    fun halfOpenOnATableTheCoverIsAboveTheFoldAndTheControlsBelow() =
        runDesktopComposeUiTest(width = PHONE, height = TALL) {
            // Density 1 in the test window: the fold's pixels are dp. High enough that the phone layout's cover
            // would cross it.
            val fold = TabletopFold(top = FOLD_TOP, bottom = FOLD_TOP + FOLD_THICKNESS)
            setContent {
                PodcastTheme {
                    CompositionLocalProvider(LocalTabletopFold provides fold) {
                        PlayerContent(
                            state = PlayerState(currentEpisode = episode("e1"), isPlaying = true),
                            actions = PlayerActions(),
                            artworkModifier = Modifier.testTag("cover"),
                        )
                    }
                }
            }
            val cover = onNodeWithTag("cover").getBoundsInRoot()
            val play = onNodeWithContentDescription(text(Res.string.pause)).getBoundsInRoot()

            assertTrue(cover.bottom.value <= fold.top, "cover $cover should be above the fold at ${fold.top}")
            assertTrue(play.top.value >= fold.bottom, "play button $play should be below the fold at ${fold.bottom}")
            val podcastName = onNodeWithText("Podcast").getBoundsInRoot()
            assertTrue(
                podcastName.bottom.value <= fold.top - Spacing.l.value,
                "the podcast's name ends at ${podcastName.bottom}, too close to the fold at ${fold.top}",
            )
            // Centered on the lower half: about as much room above the play button as below it.
            val window = onRoot().getBoundsInRoot()
            val playCenter = (play.top.value + play.bottom.value) / 2
            val halfCenter = (fold.bottom + window.bottom.value) / 2
            assertTrue(abs(playCenter - halfCenter) < CENTER_TOLERANCE, "play at $playCenter, half centered at $halfCenter")
        }

    @Test
    fun thePlayButtonIsSquareWhilePlayingAndRoundWhenPaused() = runComposeUiTest {
        var state by mutableStateOf(PlayerState(currentEpisode = episode("e1"), isPlaying = true))
        setContent { PodcastTheme { PlayerContent(state = state, actions = PlayerActions()) } }
        // Near the button's top left corner: inside a square with large corners, outside a circle.
        val bounds = onNodeWithContentDescription(text(Res.string.pause)).getBoundsInRoot()
        fun cornerPixel() = onRoot().captureToImage().toPixelMap().let { pixels ->
            val size = (bounds.right - bounds.left).value * density.density
            pixels[(bounds.left.value * density.density + size / 8).toInt(), (bounds.top.value * density.density + size / 8).toInt()]
        }
        val square = cornerPixel()

        state = state.copy(isPlaying = false)
        mainClock.advanceTimeBy(Motion.LONG.toLong())

        assertNotEquals(square, cornerPixel())
    }

    @Test
    fun beforeTheAudioLoadsTheBarUsesTheFeedDuration() = runComposeUiTest {
        // 60 s in the feed; the player has not reported a duration yet.
        val queue = listOf(episode("1"))
        setContent {
            PodcastTheme {
                PlayerContent(
                    state = PlayerState(currentEpisode = queue.first(), queue = queue, position = 22_000L, duration = null),
                    actions = PlayerActions(),
                )
            }
        }
        onNodeWithText("−0:38").assertExists()
    }

    @Test
    fun withoutAnyDurationTheBarIsEmptyAndShowsNoRemainingTime() = runComposeUiTest {
        val queue = listOf(episode("1").copy(duration = 0))
        setContent {
            PodcastTheme {
                PlayerContent(
                    state = PlayerState(currentEpisode = queue.first(), queue = queue, position = 22_000L, duration = null),
                    actions = PlayerActions(),
                )
            }
        }
        onNodeWithText("−0:00").assertDoesNotExist()
        onNodeWithText("0:22").assertExists()
    }

    @Test
    fun aShortWindowStillReachesTheBottomButtons() = runComposeUiTest {
        val queue = listOf(episode("1"))
        setContent {
            PodcastTheme {
                Box(modifier = Modifier.size(width = 400.dp, height = 360.dp)) {
                    PlayerContent(state = PlayerState(currentEpisode = queue.first(), queue = queue), actions = PlayerActions())
                }
            }
        }
        onNodeWithContentDescription(text(Res.string.sleep_timer)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playerDisablesNextOnTheLastEpisodeOfTheQueue() = runComposeUiTest {
        val queue = listOf(episode("1"), episode("2"))
        setContent {
            PodcastTheme {
                PlayerContent(state = PlayerState(currentEpisode = queue.last(), queue = queue), actions = PlayerActions())
            }
        }
        onNodeWithContentDescription(text(Res.string.next)).assertIsNotEnabled()
        onNodeWithContentDescription(text(Res.string.previous)).assertIsEnabled()
    }


    @Test
    fun activeSleepTimerIsAnnouncedNotJustColored() = runComposeUiTest {
        val queue = listOf(episode("1"))
        var timer by mutableStateOf<SleepTimer?>(null)
        setContent {
            PodcastTheme {
                PlayerContent(
                    state = PlayerState(currentEpisode = queue.first(), queue = queue, sleepTimer = timer),
                    actions = PlayerActions(),
                )
            }
        }
        onNode(hasStateDescription(text(Res.string.state_on))).assertDoesNotExist()
        // The end-of-episode timer has no countdown and must still show as on.
        timer = SleepTimer.EndOfEpisode
        waitForIdle()
        onNode(hasStateDescription(text(Res.string.state_on))).assertExists()
    }
}
