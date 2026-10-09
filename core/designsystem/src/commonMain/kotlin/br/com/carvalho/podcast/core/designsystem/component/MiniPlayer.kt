package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.generated.resources.Res
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_open_player
import org.jetbrains.compose.resources.stringResource

/** Compact player docked above the navigation; the bottom edge is the episode progress. */
@Composable
fun MiniPlayer(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Lets the navigation share the cover with the full player. */
    artworkModifier: Modifier = Modifier,
) {
    val artworkSize = 44.dp
    val elevation = 3.dp
    val openLabel = stringResource(Res.string.ds_open_player)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = elevation,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l, vertical = Spacing.s)
            .height(Sizes.miniPlayerHeight)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = openLabel, onClick = onClick),
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = Spacing.s, end = Spacing.s).align(Alignment.CenterStart),
            ) {
                PodcastArtwork(
                    imageUrl = imageUrl,
                    contentDescription = null,
                    shape = MaterialTheme.shapes.small,
                    modifier = artworkModifier.size(artworkSize),
                )
                Spacer(Modifier.width(Spacing.m))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.s))
                PlayPauseButton(isPlaying = isPlaying, isLoading = isLoading, onClick = onPlayPause)
            }
            EpisodeProgressBar(progress = progress, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
