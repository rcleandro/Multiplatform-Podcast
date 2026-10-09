package br.com.carvalho.podcast.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material.icons.rounded.RemoveFromQueue
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.component.DownloadState
import br.com.carvalho.podcast.core.designsystem.component.EpisodePlayback
import br.com.carvalho.podcast.core.designsystem.component.EpisodeRow
import br.com.carvalho.podcast.core.designsystem.component.ItemAction
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add_to_queue
import br.com.carvalho.podcast.core.ui.generated.resources.cancel_download
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download
import br.com.carvalho.podcast.core.ui.generated.resources.dismiss_new
import br.com.carvalho.podcast.core.ui.generated.resources.download_cd
import br.com.carvalho.podcast.core.ui.generated.resources.episode_options
import br.com.carvalho.podcast.core.ui.generated.resources.go_to_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_as_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.play_next
import br.com.carvalho.podcast.core.ui.generated.resources.remaining_time
import br.com.carvalho.podcast.core.ui.generated.resources.remove_from_queue
import br.com.carvalho.podcast.core.ui.generated.resources.retry_download
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.presentation.format.explicitLabel
import br.com.carvalho.podcast.presentation.format.relativeTime
import br.com.carvalho.podcast.presentation.format.text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Maps an [Episode] and its download status to the design system [EpisodeRow], with its "⋮" menu: play or pause,
 * the download action of its state, and, where the screen offers them, mark as played or unplayed, mark it and the
 * older ones as played or unplayed, go to its podcast, and play it next, add it to the queue or take it out (18.6).
 */
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
    onCancelDownloadClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onMarkPlayed: (() -> Unit)? = null,
    onMarkUnplayed: (() -> Unit)? = null,
    onMarkOlderPlayed: (() -> Unit)? = null,
    onMarkOlderUnplayed: (() -> Unit)? = null,
    onGoToPodcast: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onDismissNew: (() -> Unit)? = null,
) {
    val durationMs = episode.duration * AppConfig.MILLIS_PER_SECOND
    val progress = if (durationMs > 0) episode.playbackPosition.toFloat() / durationMs else 0f
    val remaining = episode.remainingDuration()?.let { stringResource(Res.string.remaining_time, it) }
    val published = relativeTime(episode.publishDate, getCurrentTimestamp())?.text()
    val metadata = listOfNotNull(
        explicitLabel(episode.explicit),
        podcastTitle,
        published,
        remaining ?: episode.duration.toDuration(),
    )
        .joinToString(" · ")
    val downloadState = downloadStatus.toDownloadState(episode.isDownloaded).takeIf { supportsDownloads }

    val actions = listOfNotNull(
        if (isPlaying) {
            ItemAction(stringResource(Res.string.pause), Icons.Rounded.Pause, onPlayClick)
        } else {
            ItemAction(stringResource(Res.string.play), Icons.Rounded.PlayArrow, onPlayClick)
        },
        optionalAction(Res.string.play_next, Icons.Rounded.QueuePlayNext, onPlayNext),
        optionalAction(Res.string.add_to_queue, Icons.AutoMirrored.Rounded.PlaylistAdd, onAddToQueue),
        downloadState?.let { downloadAction(it, onDownloadClick, onCancelDownloadClick, onDeleteClick) },
        // Played or not, the menu offers the opposite.
        optionalAction(Res.string.mark_as_played, Icons.Rounded.Done, onMarkPlayed.takeIf { !episode.isPlayed }),
        optionalAction(
            Res.string.mark_as_unplayed,
            Icons.Rounded.RemoveCircleOutline,
            onMarkUnplayed.takeIf { episode.isPlayed },
        ),
        optionalAction(Res.string.mark_older_as_played, Icons.Rounded.DoneAll, onMarkOlderPlayed),
        optionalAction(Res.string.mark_older_as_unplayed, Icons.Rounded.RemoveDone, onMarkOlderUnplayed),
        optionalAction(Res.string.dismiss_new, Icons.Rounded.VisibilityOff, onDismissNew),
        optionalAction(Res.string.go_to_podcast, Icons.Rounded.Podcasts, onGoToPodcast),
        optionalAction(Res.string.remove_from_queue, Icons.Rounded.RemoveFromQueue, onRemoveFromQueue),
    )

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
        downloadState = downloadState,
        onClick = onClick,
        actions = actions,
        actionsLabel = stringResource(Res.string.episode_options),
        modifier = modifier,
    )
}

/** An action the screen may leave out: none when it passes no [onClick]. */
@Composable
private fun optionalAction(label: StringResource, icon: ImageVector, onClick: (() -> Unit)?): ItemAction? =
    onClick?.let { ItemAction(stringResource(label), icon, it) }

/** The one download action that makes sense in [state]; the episode screen shows it as a button. */
@Composable
fun downloadAction(
    state: DownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
): ItemAction = when (state) {
    DownloadState.Idle -> ItemAction(stringResource(Res.string.download_cd), Icons.Rounded.Download, onDownload)
    DownloadState.Queued, is DownloadState.Downloading ->
        ItemAction(stringResource(Res.string.cancel_download), Icons.Rounded.Close, onCancel)
    DownloadState.Downloaded -> ItemAction(stringResource(Res.string.delete_download), Icons.Rounded.Delete, onRemove)
    DownloadState.Failed -> ItemAction(stringResource(Res.string.retry_download), Icons.Rounded.Refresh, onDownload)
}

fun DownloadStatus.toDownloadState(isDownloaded: Boolean): DownloadState = when (this) {
    is DownloadStatus.Queued -> DownloadState.Queued
    is DownloadStatus.Downloading -> DownloadState.Downloading(progress)
    is DownloadStatus.Completed -> DownloadState.Downloaded
    is DownloadStatus.Failed -> DownloadState.Failed
    DownloadStatus.Idle -> if (isDownloaded) DownloadState.Downloaded else DownloadState.Idle
}

private const val SECONDS_PER_MINUTE = 60

/** What is left of a started episode, in the same style as its length ("1h 5min"); null when not started or done. */
fun Episode.remainingDuration(): String? {
    val durationMs = duration * AppConfig.MILLIS_PER_SECOND
    if (isPlayed || playbackPosition <= 0 || durationMs <= 0) return null
    val leftSeconds = ((durationMs - playbackPosition) / AppConfig.MILLIS_PER_SECOND)
        .coerceAtLeast(SECONDS_PER_MINUTE.toLong())
    return leftSeconds.toDuration()
}
