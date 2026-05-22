package br.com.carvalho.podcast.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.extensions.toDate
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.app_icon
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
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
    val titleColor = if (episode.isPlayed) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val subtitle = remember(podcastTitle, episode.duration, episode.publishDate, episode.isPlayed, episode.playbackPosition) {
        val parts = mutableListOf<String>()
        podcastTitle?.let { parts.add(it) }
        parts.add(episode.duration.toDuration())
        parts.add(episode.publishDate.toDate())

        if (episode.isPlayed) {
            parts.add("Finalizado")
        } else if (episode.playbackPosition > 0 && episode.duration > 0) {
            val remainingMs = (episode.duration * 1000) - episode.playbackPosition
            val remainingMin = (remainingMs / (1000 * 60)).coerceAtLeast(1)
            parts.add("$remainingMin min restante")
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = "podcast image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = if (episode.isPlayed) 0.5f else 1f,
                placeholder = painterResource(Res.drawable.app_icon),
                error = painterResource(Res.drawable.app_icon)
            )
            if (episode.isPlayed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Finalizado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (episode.playbackPosition > 0 && episode.duration > 0) {
                val progress = episode.playbackPosition.toFloat() / (episode.duration * 1000).toFloat()
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = titleColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (episode.isPlayed) 0.6f else 1f
                    )
                )
            }
        }

        // Status de Download
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            when (downloadStatus) {
                is DownloadStatus.Downloading -> {
                    CircularProgressIndicator(
                        progress = { downloadStatus.progress },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
                is DownloadStatus.Completed -> {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Excluir download",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                is DownloadStatus.Queued -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    if (!episode.isDownloaded) {
                        IconButton(onClick = onDownloadClick) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Baixar",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Excluir download",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Ouvir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
