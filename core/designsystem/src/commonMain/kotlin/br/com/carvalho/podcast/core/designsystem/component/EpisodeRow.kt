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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_download_failed_label
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_downloaded_label
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_downloading_label
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_loading
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_new
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_played
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_playing
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_queued_label
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
 * The one episode row for podcast detail and the Episodes tab. Tapping it opens the episode; everything else
 * (play, download, mark as played) is in the sheet of its "⋮" button, also opened by a long press or a right click.
 * [metadata] is the already formatted line ("3 days · 47min"); the row adds the "Playing", "New", "Played" and
 * download markers.
 * A null [downloadState] leaves out the download markers, for platforms without downloads.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeRow(
    title: String,
    metadata: String,
    imageUrl: String?,
    playback: EpisodePlayback,
    downloadState: DownloadState?,
    onClick: () -> Unit,
    actions: List<ItemAction>,
    actionsLabel: String,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = modifier
            .fillMaxWidth()
            .onSecondaryClick { menuOpen = true }
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true }, onLongClickLabel = actionsLabel)
            .padding(start = Spacing.l, top = Spacing.s, bottom = Spacing.s),
    ) {
        PodcastArtwork(
            imageUrl = imageUrl,
            contentDescription = null,
            shape = MaterialTheme.shapes.small,
            dimmed = playback.isPlayed,
            modifier = Modifier.size(Sizes.artworkS),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            MetadataLine(metadata = metadata, marker = marker(playback, downloadState, isNew))
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
        MoreOptionsButton(actionsLabel, onClick = { menuOpen = true })
    }
    if (menuOpen) {
        ItemActionsSheet(
            title = title,
            subtitle = metadata,
            imageUrl = imageUrl,
            actions = actions,
            onDismiss = { menuOpen = false },
        )
    }
}

/** What the row says about the episode before the metadata; the most useful one wins. */
private enum class Marker { Playing, Loading, Downloading, Queued, Failed, Played, Downloaded, New, None }

private fun marker(playback: EpisodePlayback, download: DownloadState?, isNew: Boolean): Marker = when {
    playback.isLoading -> Marker.Loading
    playback.isPlaying -> Marker.Playing
    download is DownloadState.Downloading -> Marker.Downloading
    download == DownloadState.Queued -> Marker.Queued
    download == DownloadState.Failed -> Marker.Failed
    playback.isPlayed -> Marker.Played
    download == DownloadState.Downloaded -> Marker.Downloaded
    isNew -> Marker.New
    else -> Marker.None
}

@Composable
private fun MetadataLine(metadata: String, marker: Marker) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        val style = MaterialTheme.typography.labelMedium
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        val accent = PodcastTheme.colors.accentText
        when (marker) {
            Marker.Playing -> Text(stringResource(Res.string.ds_playing), style = style, color = accent)
            Marker.Loading -> Text(stringResource(Res.string.ds_loading), style = style, color = accent)
            Marker.Downloading -> Text(stringResource(Res.string.ds_downloading_label), style = style, color = muted)
            Marker.Queued -> Text(stringResource(Res.string.ds_queued_label), style = style, color = muted)
            Marker.Failed -> Text(
                stringResource(Res.string.ds_download_failed_label),
                style = style,
                color = MaterialTheme.colorScheme.error,
            )
            Marker.Played -> {
                MarkerIcon(Icons.Rounded.CheckCircle, PodcastTheme.colors.played)
                Text(stringResource(Res.string.ds_played), style = style, color = muted)
            }
            Marker.Downloaded -> {
                MarkerIcon(Icons.Rounded.DownloadDone, PodcastTheme.colors.downloaded)
                Text(stringResource(Res.string.ds_downloaded_label), style = style, color = muted)
            }
            Marker.New -> Text(stringResource(Res.string.ds_new), style = style, color = PodcastTheme.colors.accentText)
            Marker.None -> Unit
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
