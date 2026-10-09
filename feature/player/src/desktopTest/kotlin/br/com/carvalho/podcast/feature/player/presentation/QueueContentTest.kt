package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.move_down
import br.com.carvalho.podcast.core.ui.generated.resources.drag_to_reorder
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsActions.CustomActions
import androidx.compose.ui.input.key.Key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.clear
import br.com.carvalho.podcast.core.ui.generated.resources.clear_queue
import br.com.carvalho.podcast.core.ui.generated.resources.episode_options
import br.com.carvalho.podcast.core.ui.generated.resources.queue_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.queue_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.queue_summary
import br.com.carvalho.podcast.core.ui.generated.resources.remove_from_queue
import br.com.carvalho.podcast.core.ui.generated.resources.up_next
import br.com.carvalho.podcast.domain.model.Episode
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class QueueContentTest {
    private fun text(res: StringResource) = runBlocking { getString(res) }

    private val playing = episode("playing")
    private val next = episode("next")

    @Test
    fun theEpisodePlayingComesFirstThenWhatIsNextWithWhatIsLeft() = runComposeUiTest {
        setContent { PodcastTheme { QueueContent(QueueUiState(current = playing, upNext = listOf(next)), QueueActions()) } }

        onNodeWithText("Episode playing").assertIsDisplayed()
        onNodeWithText("Episode next").assertIsDisplayed()
        // "Up next · 1 episode · 30min", in the runner's language.
        val summary = runBlocking { getPluralString(Res.plurals.queue_summary, 1, 1, "30min") }
        onNodeWithText("${text(Res.string.up_next)} · $summary", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun withNothingNextTheQueueSaysHowToAddSome() = runComposeUiTest {
        setContent { PodcastTheme { QueueContent(QueueUiState(current = playing), QueueActions()) } }

        onNodeWithText("Episode playing").assertIsDisplayed()
        onNodeWithText(text(Res.string.queue_empty_message)).assertIsDisplayed()
    }

    @Test
    fun anEmptyQueueSaysHowToFillIt() = runComposeUiTest {
        setContent { PodcastTheme { QueueContent(QueueUiState(), QueueActions()) } }

        onNodeWithText(text(Res.string.queue_empty_title)).assertIsDisplayed()
        onAllNodesWithContentDescription(text(Res.string.clear_queue)).assertCountEquals(0)
    }

    @Test
    fun anEpisodeLeavesTheQueueFromItsMenuButThePlayingOneHasNoSuchOption() = runComposeUiTest {
        var removed: Episode? = null
        setContent {
            PodcastTheme {
                QueueContent(QueueUiState(current = playing, upNext = listOf(next)), QueueActions(onRemove = { removed = it }))
            }
        }

        onAllNodesWithContentDescription(text(Res.string.episode_options))[1].performClick()
        onNodeWithText(text(Res.string.remove_from_queue)).performClick()
        assertEquals("next", removed?.id)

        onAllNodesWithContentDescription(text(Res.string.episode_options))[0].performClick()
        onAllNodesWithText(text(Res.string.remove_from_queue)).assertCountEquals(0)
    }

    @Test
    fun clearingTheQueueAsksFirst() = runComposeUiTest {
        var requested = false
        var confirmed = false
        var state by mutableStateOf(QueueUiState(current = playing, upNext = listOf(next)))
        setContent {
            PodcastTheme {
                QueueContent(state, QueueActions(onRequestClear = { requested = true }, onConfirmClear = { confirmed = true }))
            }
        }
        onNodeWithContentDescription(text(Res.string.clear_queue)).performClick()
        assertTrue(requested)

        state = state.copy(isConfirmingClear = true)
        onNodeWithText(text(Res.string.clear)).performClick()

        assertTrue(confirmed)
    }

    @Test
    fun tappingAnEpisodePlaysIt() = runComposeUiTest {
        var played: Episode? = null
        setContent {
            PodcastTheme {
                QueueContent(QueueUiState(current = playing, upNext = listOf(next)), QueueActions(onPlay = { played = it }))
            }
        }

        onNodeWithText("Episode next").performClick()

        assertEquals("next", played?.id)
    }

    @Test
    fun whatComesNextMovesWithoutDraggingByScreenReaderOrKeyboard() = runComposeUiTest {
        val third = episode("third")
        var order: List<String>? = null
        setContent {
            PodcastTheme {
                QueueContent(
                    QueueUiState(current = playing, upNext = listOf(next, third)),
                    QueueActions(onReorder = { order = it }),
                )
            }
        }
        onAllNodesWithContentDescription(text(Res.string.drag_to_reorder)).assertCountEquals(2)

        val row = onNode(hasAnyDescendant(hasText("Episode next")) and SemanticsMatcher.keyIsDefined(CustomActions))
        val moveDown = row.fetchSemanticsNode().config[CustomActions].first { it.label == text(Res.string.move_down) }
        runOnIdle { moveDown.action() }
        assertEquals(listOf("third", "next"), order)

        order = null
        onNodeWithText("Episode third").requestFocus().performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.DirectionUp)
            keyUp(Key.AltLeft)
        }
        assertEquals(listOf("third", "next"), order)
    }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p", podcastTitle = "Podcast", title = "Episode $id", description = null,
        audioUrl = "", imageUrl = null, duration = 1800, publishDate = 0, isPlayed = false, playbackPosition = 0,
        isDownloaded = false, fileSize = null,
    )
}
