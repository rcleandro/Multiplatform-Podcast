package br.com.carvalho.podcast.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.component.DownloadState
import br.com.carvalho.podcast.core.designsystem.component.EpisodePlayback
import br.com.carvalho.podcast.core.designsystem.component.EpisodeRow
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.presentation.format.relativeTime
import br.com.carvalho.podcast.presentation.format.text
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.episode_options
import br.com.carvalho.podcast.core.ui.generated.resources.remaining_time
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
    val remaining = episode.remainingDuration()?.let { stringResource(Res.string.remaining_time, it) }
    val published = relativeTime(episode.publishDate, getCurrentTimestamp())?.text()
    val metadata = listOfNotNull(podcastTitle, published, remaining ?: episode.duration.toDuration())
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
        downloadState = downloadStatus.toDownloadState(episode.isDownloaded).takeIf { supportsDownloads },
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

/** What is left of a started episode, in the same style as its length ("1h 5min"); null when not started or done. */
internal fun Episode.remainingDuration(): String? {
    val durationMs = duration * AppConfig.MILLIS_PER_SECOND
    if (isPlayed || playbackPosition <= 0 || durationMs <= 0) return null
    val leftSeconds = ((durationMs - playbackPosition) / AppConfig.MILLIS_PER_SECOND)
        .coerceAtLeast(SECONDS_PER_MINUTE.toLong())
    return leftSeconds.toDuration()
}
