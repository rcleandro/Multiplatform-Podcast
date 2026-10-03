package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.generated.resources.Res
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_downloaded_label
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_new
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_played
import org.jetbrains.compose.resources.stringResource

/** What the row knows about playback of its episode. */
data class EpisodePlayback(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    /** 0..1 of the episode already played; 0 when not started. */
    val progress: Float = 0f,
    val isPlayed: Boolean = false,
)

/**
 * The one episode row for podcast detail, search, downloads and new episodes. [metadata] is the already
 * formatted line ("3 days · 47 min"); the row adds the "New", "Played" and "Downloaded" markers itself.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeRow(
    title: String,
    metadata: String,
    imageUrl: String?,
    playback: EpisodePlayback,
    downloadState: DownloadState,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = onLongClickLabel)
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        PodcastArtwork(
            imageUrl = imageUrl,
            contentDescription = null,
            shape = MaterialTheme.shapes.small,
            dimmed = playback.isPlayed,
            modifier = Modifier.size(Sizes.artworkS),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            MetadataLine(
                metadata = metadata,
                isNew = isNew && !playback.isPlayed,
                isPlayed = playback.isPlayed,
                isDownloaded = downloadState == DownloadState.Downloaded,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.let {
                    if (playback.isPlayed) it.copy(fontWeight = FontWeight.Medium) else it
                },
                color = if (playback.isPlayed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (playback.progress > 0f && !playback.isPlayed) {
                EpisodeProgressBar(progress = playback.progress, modifier = Modifier.padding(top = Spacing.xs))
            }
        }
        DownloadButton(
            state = downloadState,
            onDownload = onDownload,
            onCancel = onCancelDownload,
            onRemove = onRemoveDownload,
        )
        PlayPauseButton(
            isPlaying = playback.isPlaying,
            isLoading = playback.isLoading,
            onClick = onPlay,
            style = PlayButtonStyle.Tonal,
            progress = if (playback.isPlayed) 0f else playback.progress,
        )
    }
}

@Composable
private fun MetadataLine(metadata: String, isNew: Boolean, isPlayed: Boolean, isDownloaded: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        val style = MaterialTheme.typography.labelMedium
        if (isNew) Text(stringResource(Res.string.ds_new), style = style, color = PodcastTheme.colors.accentText)
        if (isPlayed) {
            MarkerIcon(Icons.Rounded.CheckCircle, PodcastTheme.colors.played)
            Text(
                text = stringResource(Res.string.ds_played),
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isDownloaded) {
            MarkerIcon(Icons.Rounded.DownloadDone, PodcastTheme.colors.downloaded)
            Text(
                text = stringResource(Res.string.ds_downloaded_label),
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = metadata,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MarkerIcon(icon: ImageVector, tint: Color) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(Sizes.iconS))
}
