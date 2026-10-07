package br.com.carvalho.podcast.feature.library.presentation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.library_move_down
import br.com.carvalho.podcast.core.ui.generated.resources.library_reorder
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.Podcast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class LibraryOrderContentTest {

    private fun text(res: StringResource) = runBlocking { getString(res) }

    private val entries = listOf("A", "B", "C").map {
        LibraryEntry(Podcast(it, it, "", null, null, null, emptyList(), it, null, 0, true), 0, null)
    }

    @Test
    fun everyPodcastHasADragHandleAndCanMoveWithoutDragging() = runComposeUiTest {
        var order: List<String>? = null
        setContent { PodcastTheme { LibraryOrderContent(entries, onReorder = { order = it }, onBack = {}) } }

        onAllNodesWithContentDescription(text(Res.string.library_reorder)).assertCountEquals(entries.size)
        val moveDown = onNodeWithText("A").fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .first { it.label == text(Res.string.library_move_down) }
        runOnIdle { moveDown.action() }

        assertEquals(listOf("B", "A", "C"), order)
    }

    @Test
    fun backLeavesTheScreen() = runComposeUiTest {
        var back = 0
        setContent { PodcastTheme { LibraryOrderContent(entries, onReorder = {}, onBack = { back++ }) } }

        onNodeWithContentDescription(text(Res.string.back)).performClick()

        assertEquals(1, back)
    }
}
