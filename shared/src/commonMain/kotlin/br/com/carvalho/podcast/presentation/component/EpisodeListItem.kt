package br.com.carvalho.podcast.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import coil3.compose.AsyncImage

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
    onDownloadClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val subtitle = remember(podcastTitle, episode.duration, episode.publishDate) {
        listOfNotNull(
            podcastTitle,
            episode.duration.toDuration(),
            episode.publishDate.toDate()
        ).joinToString(" • ")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            episode.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "podcast image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
