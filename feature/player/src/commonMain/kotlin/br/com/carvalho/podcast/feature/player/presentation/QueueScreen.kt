package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.core.designsystem.component.PodcastSnackbarHost
import br.com.carvalho.podcast.core.designsystem.component.SectionTitle
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.clear
import br.com.carvalho.podcast.core.ui.generated.resources.clear_queue
import br.com.carvalho.podcast.core.ui.generated.resources.clear_queue_message
import br.com.carvalho.podcast.core.ui.generated.resources.now_playing
import br.com.carvalho.podcast.core.ui.generated.resources.queue
import br.com.carvalho.podcast.core.ui.generated.resources.queue_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.queue_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.queue_summary
import br.com.carvalho.podcast.core.ui.generated.resources.removed_from_queue
import br.com.carvalho.podcast.core.ui.generated.resources.undo
import br.com.carvalho.podcast.core.ui.generated.resources.up_next
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The queue (18.6), over the player or the tabs: what is playing and what comes next. */
@Composable
fun QueueScreen(onBack: () -> Unit, viewModel: QueueViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    QueueContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = QueueActions(
            onBack = onBack,
            onPlay = { viewModel.onIntent(QueueIntent.Play(it)) },
            onRemove = { episode ->
                val index = state.upNext.indexOfFirst { it.id == episode.id }
                viewModel.onIntent(QueueIntent.Remove(episode))
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = getString(Res.string.removed_from_queue),
                        actionLabel = getString(Res.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(QueueIntent.Restore(episode, index))
                    }
                }
            },
            onRequestClear = { viewModel.onIntent(QueueIntent.RequestClear) },
            onConfirmClear = { viewModel.onIntent(QueueIntent.ConfirmClear) },
            onDismissClear = { viewModel.onIntent(QueueIntent.DismissClear) },
            onDownload = { viewModel.onIntent(QueueIntent.Download(it)) },
            onCancelDownload = { viewModel.onIntent(QueueIntent.CancelDownload(it)) },
            onDeleteDownload = { viewModel.onIntent(QueueIntent.DeleteDownload(it)) },
        ),
    )
}

data class QueueActions(
    val onBack: () -> Unit = {},
    val onPlay: (Episode) -> Unit = {},
    val onRemove: (Episode) -> Unit = {},
    val onRequestClear: () -> Unit = {},
    val onConfirmClear: () -> Unit = {},
    val onDismissClear: () -> Unit = {},
    val onDownload: (Episode) -> Unit = {},
    val onCancelDownload: (Episode) -> Unit = {},
    val onDeleteDownload: (Episode) -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueContent(
    state: QueueUiState,
    actions: QueueActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.queue), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (state.upNext.isNotEmpty()) {
                        IconButton(onClick = actions.onRequestClear) {
                            Icon(Icons.Rounded.ClearAll, contentDescription = stringResource(Res.string.clear_queue))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { PodcastSnackbarHost(snackbarHostState, clearMiniPlayer = false) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.current == null && state.upNext.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = stringResource(Res.string.queue_empty_title),
                message = stringResource(Res.string.queue_empty_message),
                modifier = Modifier.padding(padding),
            )
        } else {
            val listPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = Sizes.listBottomInset)
            QueueList(state, actions, listPadding)
        }
    }

    if (state.isConfirmingClear) {
        ConfirmDialog(
            title = stringResource(Res.string.clear_queue),
            message = stringResource(Res.string.clear_queue_message),
            confirmLabel = stringResource(Res.string.clear),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = actions.onConfirmClear,
            onDismiss = actions.onDismissClear,
        )
    }
}

@Composable
private fun QueueList(state: QueueUiState, actions: QueueActions, padding: PaddingValues) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
        state.current?.let { current ->
            item(key = NOW_PLAYING_KEY) { ListTitle(stringResource(Res.string.now_playing)) }
            item(key = "current-${current.id}") {
                QueueItem(current, state, actions, onRemove = null)
            }
        }
        if (state.upNext.isEmpty()) {
            // Nothing next yet: how to add some, in place of an empty list.
            item(key = UP_NEXT_KEY) {
                Text(
                    stringResource(Res.string.queue_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.readableWidth().padding(horizontal = Spacing.l, vertical = Spacing.l),
                )
            }
        } else {
            item(key = UP_NEXT_KEY) {
                val summary = pluralStringResource(
                    Res.plurals.queue_summary,
                    state.upNext.size,
                    state.upNext.size,
                    remainingSeconds(state.upNext).toDuration(),
                )
                ListTitle("${stringResource(Res.string.up_next)} · $summary")
            }
        }
        itemsIndexed(state.upNext, key = { _, episode -> episode.id }) { _, episode ->
            SwipeToRemove(onRemove = { actions.onRemove(episode) }) {
                QueueItem(episode, state, actions, onRemove = { actions.onRemove(episode) })
            }
        }
    }
}

@Composable
private fun QueueItem(episode: Episode, state: QueueUiState, actions: QueueActions, onRemove: (() -> Unit)?) {
    val isCurrent = episode.id == state.current?.id
    EpisodeListItem(
        episode = episode,
        onClick = { actions.onPlay(episode) },
        onPlayClick = { actions.onPlay(episode) },
        podcastTitle = episode.podcastTitle,
        isPlaying = isCurrent && state.isPlaying,
        isBuffering = isCurrent && state.isBuffering,
        downloadStatus = state.downloads[episode.id] ?: DownloadStatus.Idle,
        onDownloadClick = { actions.onDownload(episode) },
        onCancelDownloadClick = { actions.onCancelDownload(episode) },
        onDeleteClick = { actions.onDeleteDownload(episode) },
        onRemoveFromQueue = onRemove,
        modifier = Modifier.readableWidth().background(MaterialTheme.colorScheme.background),
    )
}

/** Swiped away either way, an episode leaves the queue; the menu does the same without swiping. */
@Composable
private fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val swipe = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = swipe,
        backgroundContent = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer)) },
        onDismiss = { if (it != SwipeToDismissBoxValue.Settled) onRemove() },
    ) { content() }
}

@Composable
private fun ListTitle(text: String) = SectionTitle(
    text,
    Modifier.readableWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
)

/** What is left to hear of [episodes], in seconds: whole ones not started, the rest of the started ones. */
internal fun remainingSeconds(episodes: List<Episode>): Long = episodes.sumOf { episode ->
    if (episode.isPlayed) {
        0L
    } else {
        (episode.duration - episode.playbackPosition / AppConfig.MILLIS_PER_SECOND).coerceAtLeast(0)
    }
}

private const val NOW_PLAYING_KEY = "now-playing"
private const val UP_NEXT_KEY = "up-next"
