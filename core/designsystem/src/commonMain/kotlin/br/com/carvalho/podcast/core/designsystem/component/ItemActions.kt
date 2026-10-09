package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import kotlinx.coroutines.launch

/** One entry of an item's "more options" menu; the sheet shows its [icon], the dropdown too when it has one. */
data class ItemAction(val label: String, val icon: ImageVector? = null, val onClick: () -> Unit)

/**
 * The "⋮" button of a list item and its menu. The item opens the same menu on a long press and on a right click
 * ([onSecondaryClick]), so every action is reachable without knowing the gesture.
 */
@Composable
fun ItemActionsButton(
    actions: List<ItemAction>,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        MoreOptionsButton(label, onClick = { onExpandedChange(true) })
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = action.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
                    onClick = {
                        onExpandedChange(false)
                        action.onClick()
                    },
                )
            }
        }
    }
}

/** The "⋮" button alone, for items that show their actions somewhere other than a dropdown. */
@Composable
fun MoreOptionsButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Rounded.MoreVert, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * An item's actions in a bottom sheet, under a header that says which item they act on: its artwork, [title] and
 * [subtitle]. Choosing an action runs it and closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemActionsSheet(
    title: String,
    subtitle: String,
    imageUrl: String?,
    actions: List<ItemAction>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
        ) {
            PodcastArtwork(
                imageUrl = imageUrl,
                contentDescription = null,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(Sizes.artworkS),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.s))
        Column(modifier = Modifier.padding(bottom = Spacing.l)) {
            actions.forEach { action ->
                ListItem(
                    headlineContent = { Text(action.label) },
                    leadingContent = action.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
                    colors = ListItemDefaults.colors(containerColor = BottomSheetDefaults.ContainerColor),
                    modifier = Modifier.clickable {
                        action.onClick()
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                )
            }
        }
    }
}

/** Runs [onClick] on a right click (secondary mouse button); touch screens ignore it. */
fun Modifier.onSecondaryClick(onClick: () -> Unit): Modifier = pointerInput(onClick) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.forEach { it.consume() }
                onClick()
            }
        }
    }
}
