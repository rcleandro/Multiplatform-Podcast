package br.com.carvalho.podcast.feature.podcast.presentation

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.back
import br.com.carvalho.podcast.shared.cancel
import br.com.carvalho.podcast.shared.delete
import br.com.carvalho.podcast.shared.delete_download
import br.com.carvalho.podcast.shared.delete_download_confirmation
import br.com.carvalho.podcast.shared.filter_all
import br.com.carvalho.podcast.shared.filter_downloaded
import br.com.carvalho.podcast.shared.filter_unplayed
import br.com.carvalho.podcast.shared.mark_as_played
import br.com.carvalho.podcast.shared.mark_as_played_description
import br.com.carvalho.podcast.shared.only_this_one
import br.com.carvalho.podcast.shared.podcast
import br.com.carvalho.podcast.shared.refresh
import br.com.carvalho.podcast.shared.this_and_all_below
import org.jetbrains.compose.resources.getString
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

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(getString(it))
            viewModel.clearError()
        }
    }

    PodcastDetailContent(
        state = uiState,
        episodes = pagedEpisodes,
        playerState = playerState,
        activeDownloads = activeDownloads,
        snackbarHostState = snackbarHostState,
        actions = PodcastDetailActions(
            onBack = onBackClick,
            onRefresh = viewModel::refresh,
            onFilterSelected = viewModel::setFilter,
            onEpisodeClick = { onEpisodeClick(it.id, it.podcastId) },
            onEpisodeLongClick = viewModel::onSelectEpisode,
            onPlay = viewModel::playEpisode,
            onDownload = viewModel::downloadEpisode,
            onCancelDownload = { viewModel.cancelDownload(it.id) },
            onRemoveDownload = viewModel::showDeleteConfirmation,
            onConfirmRemoveDownload = { viewModel.deleteDownload(it.id) },
            onDismissRemoveDownload = viewModel::hideDeleteConfirmation,
            onMarkPlayed = { viewModel.markAsPlayed(it.id) },
            onMarkOlderPlayed = { viewModel.markOlderAsPlayed(it.publishDate) },
            onDismissMarkPlayed = { viewModel.onSelectEpisode(null) },
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

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { PodcastDetailTopBar(scrollBehavior, actions) },
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
                EpisodeList(state, episodes, playerState, activeDownloads, actions)
            }
            PodcastDetailDialogs(state, actions)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastDetailTopBar(scrollBehavior: TopAppBarScrollBehavior, actions: PodcastDetailActions) {
    TopAppBar(
        title = { Text(stringResource(Res.string.podcast)) },
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
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Sizes.listBottomInset)) {
        state.podcast?.let { podcast -> item { PodcastHeader(podcast) } }
        item {
            FilterChipRow(
                options = listOf(
                    FilterOption(stringResource(Res.string.filter_all)),
                    FilterOption(stringResource(Res.string.filter_unplayed)),
                    FilterOption(stringResource(Res.string.filter_downloaded)),
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
        HtmlText(html = podcast.description, modifier = Modifier.fillMaxWidth())
    }
}

private const val TITLE_MAX_LINES = 3
