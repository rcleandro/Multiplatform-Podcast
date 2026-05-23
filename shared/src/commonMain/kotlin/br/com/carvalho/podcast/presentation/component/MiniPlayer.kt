package br.com.carvalho.podcast.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.AppDimensions
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.app_icon
import br.com.carvalho.podcast.shared.pause
import br.com.carvalho.podcast.shared.play
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MiniPlayer(
    episode: Episode,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppDimensions.paddingNormal,
                vertical = AppDimensions.paddingMedium
            )
            .clip(MaterialTheme.shapes.medium)
            .height(AppDimensions.miniPlayerHeight)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {},
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = AppDimensions.elevationLow
    ) {
        Column {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppDimensions.spacingNormal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimensions.iconExtraLarge)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                ) {
                    AsyncImage(
                        model = episode.imageUrl,
                        contentDescription = null, // Redundante
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(Res.drawable.app_icon),
                        error = painterResource(Res.drawable.app_icon)
                    )
                }

                Spacer(modifier = Modifier.width(AppDimensions.spacingNormal))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    episode.podcastTitle?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AppDimensions.paddingMedium))

                Box(
                    modifier = Modifier.size(AppDimensions.iconExtraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        IconButton(onClick = onPlayPauseClick) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Sutil progress bar at the bottom
            val progressValue = progress.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(progressValue, 0f..1f)
                    },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )
        }
    }
}
