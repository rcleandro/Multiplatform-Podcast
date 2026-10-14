package br.com.carvalho.podcast.presentation.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.domain.model.Episode
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class EpisodeListItemTest {
    private val episode = Episode(
        id = "e1", podcastId = "p", title = "Episode 1", description = null, audioUrl = "a", imageUrl = null,
        duration = 60, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    @Test
    fun swipingRightQueuesAndSwipingLeftMarksPlayedOrUnplayed() = runComposeUiTest {
        val done = mutableListOf<String>()
        var current by mutableStateOf(episode)
        setContent {
            PodcastTheme {
                EpisodeListItem(
                    episode = current,
                    onClick = {},
                    onPlayClick = {},
                    onAddToQueue = { done += "queue" },
                    onMarkPlayed = { done += "played" },
                    onMarkUnplayed = { done += "unplayed" },
                )
            }
        }

        onNodeWithText("Episode 1").performTouchInput { swipeRight() }
        waitForIdle()
        onNodeWithText("Episode 1").performTouchInput { swipeLeft() }
        waitForIdle()
        current = episode.copy(isPlayed = true)
        waitForIdle()
        // The row came back after each swipe, so it can be swiped again.
        onNodeWithText("Episode 1").performTouchInput { swipeLeft() }
        waitForIdle()

        assertEquals(listOf("queue", "played", "unplayed"), done)
    }

    @Test
    fun aLongPressStartsTheSelectionAndThenATapChoosesInsteadOfOpening() = runComposeUiTest {
        var selections = 0
        var opened = 0
        var selecting by mutableStateOf(false)
        setContent {
            PodcastTheme {
                EpisodeListItem(
                    episode = episode,
                    onClick = { opened++ },
                    onPlayClick = {},
                    onSelect = { selections++ },
                    selecting = selecting,
                    selected = selecting,
                )
            }
        }

        onNodeWithText("Episode 1").performTouchInput { longClick() }
        assertEquals(1, selections)

        selecting = true
        onNodeWithText("Episode 1").performClick()
        assertEquals(2, selections)
        assertEquals(0, opened)
        onNode(isSelected()).assertExists()
    }
}
