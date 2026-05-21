package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.presentation.component.HtmlText
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    viewModel: PodcastDetailViewModel,
    onBackClick: () -> Unit,
    onEpisodeClick: (String, String) -> Unit,
    onPlayEpisode: (String) -> Unit
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
                title = { Text("Podcast") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar")
                    }
                },
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        PodcastHeader(uiState)
                    }

                    item {
                        FilterSection(uiState.filter) { viewModel.setFilter(it) }
                    }

                    if (uiState.filter == EpisodeFilter.ALL) {
                        items(
                            count = pagedEpisodes.itemCount,
                            key = pagedEpisodes.itemKey { it.id },
                            contentType = pagedEpisodes.itemContentType { "episode" }
                        ) { index ->
                            val episode = pagedEpisodes[index]
                            if (episode != null) {
                                EpisodeListItem(
                                    episode = episode,
                                    isPlaying = playerState.currentEpisode?.id == episode.id && playerState.isPlaying,
                                    downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
                                    onClick = { onEpisodeClick(episode.id, episode.podcastId) },
                                    onPlayClick = { onPlayEpisode(episode.id) },
                                    onDownloadClick = { viewModel.downloadEpisode(episode.id) },
                                    onDeleteClick = { viewModel.deleteDownload(episode.id) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    } else {
                        items(uiState.episodes, key = { it.id }) { episode ->
                            EpisodeListItem(
                                episode = episode,
                                isPlaying = playerState.currentEpisode?.id == episode.id && playerState.isPlaying,
                                downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
                                onClick = { onEpisodeClick(episode.id, episode.podcastId) },
                                onPlayClick = { onPlayEpisode(episode.id) },
                                onDownloadClick = { viewModel.downloadEpisode(episode.id) },
                                onDeleteClick = { viewModel.deleteDownload(episode.id) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastHeader(uiState: PodcastDetailUiState) {
    val podcast = uiState.podcast ?: return
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = podcast.imageUrl,
            contentDescription = podcast.title,
            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun FilterSection(
    currentFilter: EpisodeFilter,
    onFilterSelected: (EpisodeFilter) -> Unit
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = currentFilter.ordinal,
        edgePadding = 16.dp,
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
                            EpisodeFilter.ALL -> "Todos"
                            EpisodeFilter.UNPLAYED -> "Não ouvidos"
                            EpisodeFilter.DOWNLOADED -> "Baixados"
                        }
                    )
                }
            )
        }
    }
}
