package br.com.carvalho.podcast.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.CheckCircleOutline
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.Spacing
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
import br.com.carvalho.podcast.core.ui.generated.resources.save_episode
import br.com.carvalho.podcast.core.ui.generated.resources.select
import br.com.carvalho.podcast.core.ui.generated.resources.unsave_episode
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.presentation.format.episodeTypeLabel
import br.com.carvalho.podcast.presentation.format.explicitLabel
import br.com.carvalho.podcast.presentation.format.relativeTime
import br.com.carvalho.podcast.presentation.format.text
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Maps an [Episode] and its download status to the design system [EpisodeRow], with its "⋮" menu: play or pause,
 * the download action of its state, and, where the screen offers them, mark as played or unplayed, mark it and the
 * older ones as played or unplayed, go to its podcast, and play it next, add it to the queue or take it out (18.6).
 * Where the screen offers them, swiping right adds it to the queue and swiping left marks it played or unplayed
 * (18.11).
 * With [onSelect], a long press or "Select" in the menu starts choosing several episodes; while [selecting], a tap
 * chooses or drops this one and there is nothing to swipe.
 */
@Suppress("LongMethod") // one line per menu action and per row parameter
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
    onSelect: (() -> Unit)? = null,
    onToggleSaved: (() -> Unit)? = null,
    selecting: Boolean = false,
    selected: Boolean = false,
) {
    val durationMs = episode.duration * AppConfig.MILLIS_PER_SECOND
    val progress = if (durationMs > 0) episode.playbackPosition.toFloat() / durationMs else 0f
    val remaining = episode.remainingDuration()?.let { stringResource(Res.string.remaining_time, it) }
    val published = relativeTime(episode.publishDate, getCurrentTimestamp())?.text()
    val metadata = listOfNotNull(
        episodeTypeLabel(episode.type),
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
        if (episode.isSaved) {
            optionalAction(Res.string.unsave_episode, Icons.Rounded.BookmarkRemove, onToggleSaved)
        } else {
            optionalAction(Res.string.save_episode, Icons.Rounded.BookmarkAdd, onToggleSaved)
        },
        optionalAction(Res.string.mark_older_as_played, Icons.Rounded.DoneAll, onMarkOlderPlayed),
        optionalAction(Res.string.mark_older_as_unplayed, Icons.Rounded.RemoveDone, onMarkOlderUnplayed),
        optionalAction(Res.string.dismiss_new, Icons.Rounded.VisibilityOff, onDismissNew),
        optionalAction(Res.string.go_to_podcast, Icons.Rounded.Podcasts, onGoToPodcast),
        optionalAction(Res.string.remove_from_queue, Icons.Rounded.RemoveFromQueue, onRemoveFromQueue),
        optionalAction(Res.string.select, Icons.Rounded.CheckCircleOutline, onSelect),
    )

    SwipeActions(
        onSwipeRight = onAddToQueue.takeIf { !selecting },
        onSwipeLeft = (if (episode.isPlayed) onMarkUnplayed else onMarkPlayed).takeIf { !selecting },
        leftIcon = if (episode.isPlayed) Icons.Rounded.RemoveCircleOutline else Icons.Rounded.Done,
        modifier = modifier,
    ) {
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
            onClick = onSelect?.takeIf { selecting } ?: onClick,
            actions = actions,
            actionsLabel = stringResource(Res.string.episode_options),
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            selected = selected.takeIf { selecting },
            onLongClick = onSelect,
            longClickLabel = stringResource(if (onSelect != null) Res.string.select else Res.string.episode_options),
        )
    }
}

/** Swiping does the action of that side and puts the row back; nothing to swipe without either action. */
@Composable
private fun SwipeActions(
    onSwipeRight: (() -> Unit)?,
    onSwipeLeft: (() -> Unit)?,
    leftIcon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // The queue swipes its rows away itself.
    if (onSwipeRight == null && onSwipeLeft == null) {
        Box(modifier) { content() }
        return
    }
    val swipe = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = swipe,
        modifier = modifier,
        enableDismissFromStartToEnd = onSwipeRight != null,
        enableDismissFromEndToStart = onSwipeLeft != null,
        backgroundContent = {
            val toQueue = swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val colors = MaterialTheme.colorScheme
            Box(
                contentAlignment = if (toQueue) Alignment.CenterStart else Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (toQueue) colors.primaryContainer else colors.tertiaryContainer)
                    .padding(horizontal = Spacing.l),
            ) {
                Icon(
                    if (toQueue) Icons.AutoMirrored.Rounded.PlaylistAdd else leftIcon,
                    contentDescription = null,
                    tint = if (toQueue) colors.onPrimaryContainer else colors.onTertiaryContainer,
                )
            }
        },
        onDismiss = { side ->
            when (side) {
                SwipeToDismissBoxValue.StartToEnd -> onSwipeRight?.invoke()
                SwipeToDismissBoxValue.EndToStart -> onSwipeLeft?.invoke()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            scope.launch { swipe.reset() }
        },
    ) { content() }
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
