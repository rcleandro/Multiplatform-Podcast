package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.close
import br.com.carvalho.podcast.core.ui.generated.resources.timer_15_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_end_of_episode
import br.com.carvalho.podcast.domain.model.Chapter
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.SleepTimer
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PlayerDialogsTest {
    private fun text(res: StringResource) = runBlocking { getString(res) }

    @Test
    fun theSpeedDialogReturnsTheChosenSpeed() = runComposeUiTest {
        var chosen: Float? = null
        var dismissed = false
        setContent {
            PodcastTheme {
                SpeedSelectorDialog(currentSpeed = 1f, onSpeedSelected = { chosen = it }, onDismiss = { dismissed = true })
            }
        }

        onNodeWithText("1.5×").performClick()
        onNodeWithText(text(Res.string.cancel)).performClick()

        assertEquals(FAST, chosen)
        assertTrue(dismissed)
    }

    @Test
    fun theTimerDialogShowsTheTimeLeftAndReturnsTheChoice() = runComposeUiTest {
        var chosen: SleepTimer? = null
        val state = PlayerState(sleepTimer = SleepTimer.Minutes(QUARTER_HOUR), sleepTimerMillis = TIME_LEFT_MS)
        setContent {
            PodcastTheme {
                SleepTimerDialog(playerState = state, onTimerSelected = { chosen = it }, onDismiss = {})
            }
        }

        onNodeWithText("${text(Res.string.timer_15_min)} (1:30)").assertIsDisplayed()
        onNodeWithText(text(Res.string.timer_end_of_episode)).performClick()

        assertEquals(SleepTimer.EndOfEpisode, chosen)
    }

    @Test
    fun theChaptersDialogListsEachWithItsStartAndReturnsThePick() = runComposeUiTest {
        var chosen: Chapter? = null
        val chapters = listOf(Chapter(0, "Intro"), Chapter(3_725_000, "Questions"))
        setContent {
            PodcastTheme {
                ChaptersDialog(chapters = chapters, current = chapters[0], onChapterSelected = { chosen = it }, onDismiss = {})
            }
        }

        onNodeWithText("1:02:05").assertIsDisplayed()
        onNodeWithText("Questions").performClick()

        assertEquals(chapters[1], chosen)
    }

    private companion object {
        const val FAST = 1.5f
        const val QUARTER_HOUR = 15
        const val TIME_LEFT_MS = 90_000L
    }
}
