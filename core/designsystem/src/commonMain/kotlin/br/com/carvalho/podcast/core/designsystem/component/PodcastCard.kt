package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.generated.resources.Res
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_unplayed_count
import org.jetbrains.compose.resources.pluralStringResource

/** Library tile: cover, title and author. The badge shows unplayed episodes and hides at zero. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PodcastCard(
    title: String,
    author: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unplayedCount: Int = 0,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = onLongClickLabel)
            .padding(Spacing.xs),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            PodcastArtwork(
                imageUrl = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            if (unplayedCount > 0) {
                val badgeLabel = pluralStringResource(Res.plurals.ds_unplayed_count, unplayedCount, unplayedCount)
                Text(
                    text = unplayedCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = PodcastTheme.colors.onBrand,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.s)
                        .background(PodcastTheme.colors.brand, CircleShape)
                        .padding(horizontal = Spacing.s, vertical = Spacing.xxs)
                        .semantics { contentDescription = badgeLabel },
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        author?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
