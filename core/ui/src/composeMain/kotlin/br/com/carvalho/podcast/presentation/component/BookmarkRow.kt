package br.com.carvalho.podcast.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.extensions.toTime
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.bookmark_delete
import br.com.carvalho.podcast.domain.model.Bookmark
import org.jetbrains.compose.resources.stringResource

/** A bookmark (19.3): when it is in the episode and its note; tapping goes there, the bin deletes it. */
@Composable
fun BookmarkRow(bookmark: Bookmark, onSelect: (Bookmark) -> Unit, onDelete: (Bookmark) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onSelect(bookmark) },
    ) {
        Text(
            text = bookmark.positionMs.toTime(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(Spacing.m))
        Text(text = bookmark.note, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { onDelete(bookmark) }) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(Res.string.bookmark_delete))
        }
    }
}
