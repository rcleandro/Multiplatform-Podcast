package br.com.carvalho.podcast.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.component.DownloadState
import br.com.carvalho.podcast.core.designsystem.component.EpisodePlayback
import br.com.carvalho.podcast.core.designsystem.component.EpisodeRow
import br.com.carvalho.podcast.core.extensions.toDate
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.episode_options
import br.com.carvalho.podcast.shared.remaining_min
import org.jetbrains.compose.resources.stringResource

/** Maps an [Episode] and its download status to the design system [EpisodeRow]. */
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
    onLongClick: (() -> Unit)? = null,
    onDownloadClick: () -> Unit = {},
    onCancelDownloadClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val durationMs = episode.duration * AppConfig.MILLIS_PER_SECOND
    val progress = if (durationMs > 0) episode.playbackPosition.toFloat() / durationMs else 0f
    val remaining = if (!episode.isPlayed && episode.playbackPosition > 0 && durationMs > 0) {
        val minutes = ((durationMs - episode.playbackPosition) / (AppConfig.MILLIS_PER_SECOND * SECONDS_PER_MINUTE))
            .coerceAtLeast(1)
        stringResource(Res.string.remaining_min, minutes)
    } else {
        null
    }
    val metadata = listOfNotNull(podcastTitle, episode.publishDate.toDate(), remaining ?: episode.duration.toDuration())
        .joinToString(" · ")

    EpisodeRow(
        title = episode.title,
        metadata = metadata,
        imageUrl = episode.imageUrl,
        playback = EpisodePlayback(
            isPlaying = isPlaying,
            isLoading = isBuffering,
            progress = progress,
            isPlayed = episode.isPlayed,
        ),
        downloadState = downloadStatus.toDownloadState(episode.isDownloaded),
        onClick = onClick,
        onPlay = onPlayClick,
        onDownload = onDownloadClick,
        onCancelDownload = onCancelDownloadClick,
        onRemoveDownload = onDeleteClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClick?.let { stringResource(Res.string.episode_options) },
        modifier = modifier,
    )
}

internal fun DownloadStatus.toDownloadState(isDownloaded: Boolean): DownloadState = when (this) {
    is DownloadStatus.Queued -> DownloadState.Queued
    is DownloadStatus.Downloading -> DownloadState.Downloading(progress)
    is DownloadStatus.Completed -> DownloadState.Downloaded
    is DownloadStatus.Failed -> DownloadState.Failed
    DownloadStatus.Idle -> if (isDownloaded) DownloadState.Downloaded else DownloadState.Idle
}

private const val SECONDS_PER_MINUTE = 60
