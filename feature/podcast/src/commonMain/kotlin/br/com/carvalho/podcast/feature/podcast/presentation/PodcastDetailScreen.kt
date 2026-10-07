package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import br.com.carvalho.podcast.core.designsystem.component.PodcastSnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.designsystem.component.FilterChipRow
import br.com.carvalho.podcast.core.designsystem.component.FilterOption
import br.com.carvalho.podcast.core.designsystem.component.HtmlText
import br.com.carvalho.podcast.core.designsystem.component.LoadingState
import br.com.carvalho.podcast.core.designsystem.component.PodcastArtwork
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.presentation.MessageEffect
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.filter_all
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.filter_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played_description
import br.com.carvalho.podcast.core.ui.generated.resources.only_this_one
import br.com.carvalho.podcast.core.ui.generated.resources.show_less
import br.com.carvalho.podcast.core.ui.generated.resources.show_more
import br.com.carvalho.podcast.core.ui.generated.resources.refresh
import br.com.carvalho.podcast.core.ui.generated.resources.this_and_all_below
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PodcastDetailScreen(
    podcastId: String,
    viewModel: PodcastDetailViewModel = koinViewModel(key = podcastId) { parametersOf(podcastId) },
    onBackClick: () -> Unit,
    onEpisodeClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedEpisodes = viewModel.pagedEpisodes.collectAsLazyPagingItems()
    val playerState by viewModel.playerState.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    MessageEffect(viewModel.messages, snackbarHostState)

    PodcastDetailContent(
        state = uiState,
        episodes = pagedEpisodes,
        playerState = playerState,
        activeDownloads = activeDownloads,
        snackbarHostState = snackbarHostState,
        actions = PodcastDetailActions(
            onBack = onBackClick,
            onRefresh = { viewModel.onIntent(PodcastDetailIntent.Refresh) },
            onFilterSelected = { viewModel.onIntent(PodcastDetailIntent.SetFilter(it)) },
            onEpisodeClick = { onEpisodeClick(it.id, it.podcastId) },
            onEpisodeLongClick = { viewModel.onIntent(PodcastDetailIntent.SelectEpisode(it)) },
            onPlay = { viewModel.onIntent(PodcastDetailIntent.Play(it)) },
            onDownload = { viewModel.onIntent(PodcastDetailIntent.Download(it)) },
            onCancelDownload = { viewModel.onIntent(PodcastDetailIntent.CancelDownload(it)) },
            onRemoveDownload = { viewModel.onIntent(PodcastDetailIntent.RequestDeleteDownload(it)) },
            onConfirmRemoveDownload = { viewModel.onIntent(PodcastDetailIntent.ConfirmDeleteDownload(it)) },
            onDismissRemoveDownload = { viewModel.onIntent(PodcastDetailIntent.DismissDeleteDownload) },
            onMarkPlayed = { viewModel.onIntent(PodcastDetailIntent.MarkPlayed(it)) },
            onMarkOlderPlayed = { viewModel.onIntent(PodcastDetailIntent.MarkOlderPlayed(it)) },
            onDismissMarkPlayed = { viewModel.onIntent(PodcastDetailIntent.DismissMarkPlayed) },
        ),
    )
}

data class PodcastDetailActions(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onFilterSelected: (EpisodeFilter) -> Unit = {},
    val onEpisodeClick: (Episode) -> Unit = {},
    val onEpisodeLongClick: (Episode) -> Unit = {},
    val onPlay: (Episode) -> Unit = {},
    val onDownload: (Episode) -> Unit = {},
    val onCancelDownload: (Episode) -> Unit = {},
    val onRemoveDownload: (Episode) -> Unit = {},
    val onConfirmRemoveDownload: (Episode) -> Unit = {},
    val onDismissRemoveDownload: () -> Unit = {},
    val onMarkPlayed: (Episode) -> Unit = {},
    val onMarkOlderPlayed: (Episode) -> Unit = {},
    val onDismissMarkPlayed: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailContent(
    state: PodcastDetailUiState,
    episodes: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: PodcastDetailActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    // The header shows the name; once it scrolls away, the bar takes it over.
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { PodcastSnackbarHost(snackbarHostState) },
        topBar = { PodcastDetailTopBar(scrollBehavior, state.podcast?.title?.takeIf { headerGone }, actions) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets()
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.padding(padding)
        ) {
            if (state.isLoading) {
                LoadingState()
            } else {
                EpisodeList(state, episodes, playerState, activeDownloads, actions, listState)
            }
            PodcastDetailDialogs(state, actions)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastDetailTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String?,
    actions: PodcastDetailActions,
) {
    TopAppBar(
        title = { title?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        navigationIcon = {
            IconButton(onClick = actions.onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
            }
        },
        actions = {
            IconButton(onClick = actions.onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(Res.string.refresh))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun EpisodeList(
    state: PodcastDetailUiState,
    episodes: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: PodcastDetailActions,
    listState: LazyListState,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Sizes.listBottomInset),
    ) {
        state.podcast?.let { podcast -> item { PodcastHeader(podcast) } }
        item {
            FilterChipRow(
                // "Downloaded" stays last, so the indices still match EpisodeFilter where it is left out.
                options = listOfNotNull(
                    FilterOption(stringResource(Res.string.filter_all)),
                    FilterOption(stringResource(Res.string.filter_unplayed)),
                    FilterOption(stringResource(Res.string.filter_downloaded)).takeIf { supportsDownloads },
                ),
                selectedIndex = state.filter.ordinal,
                onSelected = { actions.onFilterSelected(EpisodeFilter.entries[it]) },
                modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
            )
        }
        items(
            count = episodes.itemCount,
            key = episodes.itemKey { it.id },
            contentType = episodes.itemContentType { "episode" }
        ) { index ->
            episodes[index]?.let { episode ->
                val isCurrent = playerState.currentEpisode?.id == episode.id
                EpisodeListItem(
                    episode = episode,
                    isBuffering = isCurrent && playerState.isBuffering,
                    isPlaying = isCurrent && playerState.isPlaying,
                    downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
                    onClick = { actions.onEpisodeClick(episode) },
                    onLongClick = { actions.onEpisodeLongClick(episode) },
                    onPlayClick = { actions.onPlay(episode) },
                    onDownloadClick = { actions.onDownload(episode) },
                    onCancelDownloadClick = { actions.onCancelDownload(episode) },
                    onDeleteClick = { actions.onRemoveDownload(episode) }
                )
            }
        }
    }
}

@Composable
private fun PodcastDetailDialogs(state: PodcastDetailUiState, actions: PodcastDetailActions) {
    state.selectedEpisode?.let { episode ->
        AlertDialog(
            onDismissRequest = actions.onDismissMarkPlayed,
            title = { Text(stringResource(Res.string.mark_as_played)) },
            text = { Text(stringResource(Res.string.mark_as_played_description, episode.title)) },
            confirmButton = {
                TextButton(onClick = { actions.onMarkPlayed(episode) }) {
                    Text(stringResource(Res.string.only_this_one))
                }
            },
            dismissButton = {
                TextButton(onClick = { actions.onMarkOlderPlayed(episode) }) {
                    Text(stringResource(Res.string.this_and_all_below))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
    state.deleteEpisodeConfirmation?.let { episode ->
        ConfirmDialog(
            title = stringResource(Res.string.delete_download),
            message = stringResource(Res.string.delete_download_confirmation, episode.title),
            confirmLabel = stringResource(Res.string.delete),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = { actions.onConfirmRemoveDownload(episode) },
            onDismiss = actions.onDismissRemoveDownload,
        )
    }
}

@Composable
private fun PodcastHeader(podcast: Podcast) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            PodcastArtwork(
                imageUrl = podcast.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(Sizes.artworkM),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
                podcast.author?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        CollapsibleDescription(podcast.description)
    }
}

/** A long description shows its first lines and "Show more"; a short one shows whole, with no button. */
@Composable
private fun CollapsibleDescription(html: String) {
    var expanded by rememberSaveable(html) { mutableStateOf(false) }
    var overflows by remember(html) { mutableStateOf(false) }
    Column {
        HtmlText(
            html = html,
            maxLines = if (expanded) Int.MAX_VALUE else DESCRIPTION_COLLAPSED_LINES,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            modifier = Modifier.fillMaxWidth(),
        )
        if (overflows || expanded) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(if (expanded) Res.string.show_less else Res.string.show_more))
            }
        }
    }
}

private const val TITLE_MAX_LINES = 3
private const val DESCRIPTION_COLLAPSED_LINES = 4
