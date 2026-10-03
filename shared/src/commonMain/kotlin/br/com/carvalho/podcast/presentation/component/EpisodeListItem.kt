package br.com.carvalho.podcast.presentation.component

import br.com.carvalho.podcast.core.designsystem.Alpha
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.extensions.toDate
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.app_icon
import br.com.carvalho.podcast.shared.delete_download_cd
import br.com.carvalho.podcast.shared.download_cd
import br.com.carvalho.podcast.shared.finished
import br.com.carvalho.podcast.shared.pause
import br.com.carvalho.podcast.shared.play
import br.com.carvalho.podcast.shared.remaining_min
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeListItem(
    episode: Episode,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    podcastTitle: String? = null,
    isBuffering: Boolean = false,
    isPlaying: Boolean = false,
    downloadStatus: DownloadStatus = DownloadStatus.Idle,
    onLongClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val progressHeight = 3.dp
    val titleColor = if (episode.isPlayed) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = Alpha.muted)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val finishedString = stringResource(Res.string.finished)
    val remainingMinFormat = stringResource(Res.string.remaining_min)

    val subtitle = remember(
        podcastTitle,
        episode.duration,
        episode.publishDate,
        episode.isPlayed,
        episode.playbackPosition,
        finishedString,
        remainingMinFormat
    ) {
        val parts = mutableListOf<String>()
        podcastTitle?.let { parts.add(it) }
        parts.add(episode.duration.toDuration())
        parts.add(episode.publishDate.toDate())

        if (episode.isPlayed) {
            parts.add(finishedString)
        } else if (episode.playbackPosition > 0 && episode.duration > 0) {
            val remainingMs = (episode.duration * AppConfig.MILLIS_PER_SECOND) - episode.playbackPosition
            val remainingMin = (remainingMs / (AppConfig.MILLIS_PER_SECOND * 60)).coerceAtLeast(1)
            parts.add(remainingMinFormat.replace("%d", remainingMin.toString()))
        }

        parts.joinToString(" • ")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics(mergeDescendants = true) {
                onLongClick(null) {
                    onLongClick()
                    true
                }
            }
            .padding(
                horizontal = Spacing.l,
                vertical = Spacing.l
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.artworkS)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = if (episode.isPlayed) Alpha.muted else 1f,
                placeholder = painterResource(Res.drawable.app_icon),
                error = painterResource(Res.drawable.app_icon)
            )
            if (episode.isPlayed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = Alpha.disabled)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(Res.string.finished),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Sizes.iconM)
                    )
                }
            } else if (episode.playbackPosition > 0 && episode.duration > 0) {
                val progress =
                    episode.playbackPosition.toFloat() / (episode.duration * AppConfig.MILLIS_PER_SECOND).toFloat()
                val progressValue = progress.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(progressHeight)
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(progressValue, 0f..1f)
                        },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.l))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = titleColor
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (episode.isPlayed) Alpha.muted else 1f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Spacing.s))

        Box(
            modifier = Modifier.size(Spacing.xxl),
            contentAlignment = Alignment.Center
        ) {
            when (downloadStatus) {
                is DownloadStatus.Downloading -> {
                    CircularProgressIndicator(
                        progress = { downloadStatus.progress },
                        modifier = Modifier.size(Sizes.iconS),
                        strokeWidth = Sizes.progressStroke,
                    )
                }

                is DownloadStatus.Completed -> {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(Res.string.delete_download_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.muted),
                            modifier = Modifier.size(Sizes.iconS)
                        )
                    }
                }

                is DownloadStatus.Queued -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Sizes.iconS),
                        strokeWidth = Sizes.progressStroke
                    )
                }

                else -> {
                    if (!episode.isDownloaded) {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = stringResource(Res.string.download_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.muted),
                                modifier = Modifier.size(Sizes.iconS)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(Res.string.delete_download_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.muted),
                                modifier = Modifier.size(Sizes.iconS)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.size(Sizes.touchTarget),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.iconS),
                    strokeWidth = Sizes.progressStroke
                )
            } else {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Sizes.iconL)
                    )
                }
            }
        }
    }
}

