package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.app_icon
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
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
                    contentPadding = PaddingValues(bottom = 80.dp)
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
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            if (uiState.selectedEpisode != null) {
                AlertDialog(
                    onDismissRequest = viewModel::onSelectEpisode,
                    title = { Text("Marcar como ouvido") },
                    text = { Text("Escolha uma opção para o episódio \"${uiState.selectedEpisode?.title}\"") },
                    confirmButton = {
                        TextButton(onClick = {
                            uiState.selectedEpisode?.let { viewModel.markAsPlayed(it.id) }
                        }) {
                            Text("Apenas este")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            uiState.selectedEpisode?.let { viewModel.markOlderAsPlayed(it.publishDate) }
                        }) {
                            Text("Este e todos abaixo")
                        }
                    }
                )
            }

            if (uiState.deleteEpisodeConfirmation != null) {
                AlertDialog(
                    onDismissRequest = viewModel::hideDeleteConfirmation,
                    title = { Text("Excluir download") },
                    text = { Text("Deseja realmente excluir o download do episódio \"${uiState.deleteEpisodeConfirmation?.title}\"?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                uiState.deleteEpisodeConfirmation?.let { viewModel.deleteDownload(it.id) }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::hideDeleteConfirmation) {
                            Text("Cancelar")
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
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))
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
