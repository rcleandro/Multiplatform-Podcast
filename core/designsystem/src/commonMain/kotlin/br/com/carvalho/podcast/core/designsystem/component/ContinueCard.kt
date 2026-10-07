package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.Alpha
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing

/**
 * "Continue listening" tile: a big cover with a ring of how much was heard, the title and what is left. Tapping it
 * plays the episode from where it stopped, so the ring holds a play icon.
 */
@Composable
fun ContinueCard(
    title: String,
    imageUrl: String?,
    progress: Float,
    caption: String?,
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
) {
    val ringSize = 36.dp
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .width(Sizes.artworkM)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = onClickLabel, onClick = onClick)
            .padding(Spacing.xs),
    ) {
        Box {
            PodcastArtwork(
                imageUrl = imageUrl,
                contentDescription = null,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.s)
                    .size(ringSize)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = Alpha.muted), CircleShape),
            ) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = PodcastTheme.colors.brand,
                    trackColor = PodcastTheme.colors.brand.copy(alpha = Alpha.scrim),
                    strokeWidth = Sizes.progressStroke,
                    gapSize = 0.dp,
                    modifier = Modifier.size(ringSize).padding(Spacing.xxs),
                )
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = PodcastTheme.colors.brand,
                    modifier = Modifier.size(Sizes.iconS),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        caption?.let {
            Text(text = it, style = MaterialTheme.typography.labelMedium, color = PodcastTheme.colors.accentText)
        }
    }
}

/** Small caps title over a group of items ("Continue listening", "Today"), read as a heading. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = PodcastTheme.typography.section,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}
