package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing

/** Library row: small cover, title, author and a line such as "2 days ago · 3 unplayed"; [dragHandle] at the end. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PodcastListItem(
    title: String,
    author: String?,
    imageUrl: String?,
    supportingText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: List<ItemAction> = emptyList(),
    actionsLabel: String? = null,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val openMenu = { menuOpen = true }.takeIf { actions.isNotEmpty() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .then(if (openMenu != null) Modifier.onSecondaryClick(openMenu) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = openMenu, onLongClickLabel = actionsLabel)
            // With a "⋮", the button's own touch area is the right margin, as in the episode rows.
            .padding(
                start = Spacing.s,
                top = Spacing.s,
                bottom = Spacing.s,
                end = if (actions.isEmpty()) Spacing.s else 0.dp,
            ),
    ) {
        PodcastArtwork(imageUrl = imageUrl, contentDescription = null, modifier = Modifier.size(Sizes.artworkS))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs), modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            listOfNotNull(author, supportingText).forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (actions.isNotEmpty()) {
            ItemActionsButton(
                actions,
                actionsLabel.orEmpty(),
                expanded = menuOpen,
                onExpandedChange = { menuOpen = it },
            )
        }
        dragHandle?.invoke()
    }
}
