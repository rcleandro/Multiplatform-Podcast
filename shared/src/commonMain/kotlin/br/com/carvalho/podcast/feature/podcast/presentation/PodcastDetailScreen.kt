package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import br.com.carvalho.podcast.core.designsystem.AppDimensions
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.presentation.component.HtmlText
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.app_icon
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
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.podcast)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(Res.string.refresh)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets()
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@PullToRefreshBox
            }

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = AppDimensions.miniPlayerHeightWithPadding)
                ) {
                    item { PodcastHeader(uiState) }

                    item { FilterSection(uiState.filter) { viewModel.setFilter(it) } }

                    items(
                        count = pagedEpisodes.itemCount,
                        key = pagedEpisodes.itemKey { it.id },
                        contentType = pagedEpisodes.itemContentType { "episode" }
                    ) { index ->
                        val episode = pagedEpisodes[index]
                        if (episode != null) {
                            EpisodeListItem(
                                episode = episode,
                                isBuffering = playerState.currentEpisode?.id == episode.id && playerState.isBuffering,
                                isPlaying = playerState.currentEpisode?.id == episode.id && playerState.isPlaying,
                                downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
                                onClick = { onEpisodeClick(episode.id, episode.podcastId) },
                                onLongClick = { viewModel.onSelectEpisode(episode) },
                                onPlayClick = { viewModel.playEpisode(episode) },
                                onDownloadClick = { viewModel.downloadEpisode(episode) },
                                onDeleteClick = { viewModel.showDeleteConfirmation(episode) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = AppDimensions.paddingNormal))
                        }
                    }
                }
            }

            if (uiState.selectedEpisode != null) {
                AlertDialog(
                    onDismissRequest = viewModel::onSelectEpisode,
                    title = { Text(stringResource(Res.string.mark_as_played)) },
                    text = {
                        Text(
                            stringResource(
                                Res.string.mark_as_played_description,
                                uiState.selectedEpisode?.title ?: ""
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            uiState.selectedEpisode?.let { viewModel.markAsPlayed(it.id) }
                        }) {
                            Text(stringResource(Res.string.only_this_one))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            uiState.selectedEpisode?.let { viewModel.markOlderAsPlayed(it.publishDate) }
                        }) {
                            Text(stringResource(Res.string.this_and_all_below))
                        }
                    }
                )
            }

            if (uiState.deleteEpisodeConfirmation != null) {
                AlertDialog(
                    onDismissRequest = viewModel::hideDeleteConfirmation,
                    title = { Text(stringResource(Res.string.delete_download)) },
                    text = {
                        Text(
                            stringResource(
                                Res.string.delete_download_confirmation,
                                uiState.deleteEpisodeConfirmation?.title ?: ""
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                uiState.deleteEpisodeConfirmation?.let { viewModel.deleteDownload(it.id) }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(Res.string.delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::hideDeleteConfirmation) {
                            Text(stringResource(Res.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PodcastHeader(uiState: PodcastDetailUiState) {
    val podcast = uiState.podcast ?: return
    Row(
        modifier = Modifier.fillMaxWidth().padding(AppDimensions.paddingNormal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(AppDimensions.headerImageSize).clip(RoundedCornerShape(AppDimensions.radiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = podcast.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(Res.drawable.app_icon),
                error = painterResource(Res.drawable.app_icon)
            )
        }
        Spacer(modifier = Modifier.width(AppDimensions.spacingNormal))
        Column {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = podcast.author ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HtmlText(
        html = podcast.description,
        modifier = Modifier.padding(
            horizontal = AppDimensions.paddingNormal,
            vertical = AppDimensions.paddingMedium
        )
    )
}

@Composable
private fun FilterSection(
    currentFilter: EpisodeFilter,
    onFilterSelected: (EpisodeFilter) -> Unit
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = currentFilter.ordinal,
        edgePadding = AppDimensions.paddingNormal,
        containerColor = MaterialTheme.colorScheme.background,
        divider = {}
    ) {
        EpisodeFilter.entries.forEach { filter ->
            Tab(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Text(
                        text = when (filter) {
                            EpisodeFilter.ALL -> stringResource(Res.string.filter_all)
                            EpisodeFilter.UNPLAYED -> stringResource(Res.string.filter_unplayed)
                            EpisodeFilter.DOWNLOADED -> stringResource(Res.string.filter_downloaded)
                        }
                    )
                }
            )
        }
    }
}
