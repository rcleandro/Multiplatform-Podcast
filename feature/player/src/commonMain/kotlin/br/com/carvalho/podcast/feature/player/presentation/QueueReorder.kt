package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.icons.DragHandle
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.drag_to_reorder
import br.com.carvalho.podcast.core.ui.generated.resources.move_down
import br.com.carvalho.podcast.core.ui.generated.resources.move_up
import br.com.carvalho.podcast.domain.model.Episode
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope

/** An episode of [episodes] at [index] that moves: by its handle, by screen reader and by keyboard (18.6). */
@Composable
internal fun ReorderableCollectionItemScope.MovableRow(
    episodes: List<Episode>,
    index: Int,
    onReorder: (List<String>) -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .readableWidth()
            .background(MaterialTheme.colorScheme.background)
            .moveActions(episodes, index, onReorder),
    ) {
        content()
        Icon(
            Icons.Rounded.DragHandle,
            contentDescription = stringResource(Res.string.drag_to_reorder),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .draggableHandle(onDragStopped = { onReorder(episodes.ids()) })
                .size(Sizes.touchTarget)
                .padding(Spacing.m),
        )
    }
}

/**
 * Reordering without dragging: "move up" and "move down" for screen readers (TalkBack, VoiceOver), and Alt+↑ / Alt+↓
 * with the episode focused on a keyboard.
 */
@Composable
private fun Modifier.moveActions(episodes: List<Episode>, index: Int, onReorder: (List<String>) -> Unit): Modifier {
    val up = stringResource(Res.string.move_up)
    val down = stringResource(Res.string.move_down)
    fun move(to: Int): Boolean {
        if (to !in episodes.indices) return false
        onReorder(episodes.moved(index, to).ids())
        return true
    }
    return onPreviewKeyEvent { event ->
        when {
            event.type != KeyEventType.KeyDown || !event.isAltPressed -> false
            event.key == Key.DirectionUp -> move(index - 1)
            event.key == Key.DirectionDown -> move(index + 1)
            else -> false
        }
    }.semantics {
        customActions = listOfNotNull(
            CustomAccessibilityAction(up) { move(index - 1) }.takeIf { index > 0 },
            CustomAccessibilityAction(down) { move(index + 1) }.takeIf { index < episodes.lastIndex },
        )
    }
}

private fun List<Episode>.moved(from: Int, to: Int) = toMutableList().apply { add(to, removeAt(from)) }

/** Moves the episode with key [from] to where [to] is; keys that are not episodes of the queue change nothing. */
internal fun List<Episode>.movedByKey(from: Any, to: Any): List<Episode> {
    val fromIndex = indexOfFirst { it.id == from }
    val toIndex = indexOfFirst { it.id == to }
    return if (fromIndex == -1 || toIndex == -1) this else moved(fromIndex, toIndex)
}

private fun List<Episode>.ids() = map { it.id }
