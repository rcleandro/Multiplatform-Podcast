package br.com.carvalho.podcast.feature.player.presentation

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
