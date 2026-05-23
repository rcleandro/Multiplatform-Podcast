package br.com.carvalho.podcast.feature.search.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = koinViewModel(),
    onEpisodeClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedResults = viewModel.pagedResults.collectAsLazyPagingItems()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val playerState by viewModel.audioPlayer.playerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(pagedResults.loadState.refresh) {
        val refreshState = pagedResults.loadState.refresh
        if (refreshState is LoadState.Error) {
            viewModel.setError("Erro ao carregar resultados")
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            placeholder = {
                                Text(
                                    text = "Buscar episódios...",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true
                        )
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar")
                        }
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(
                count = pagedResults.itemCount,
                key = pagedResults.itemKey { it.id },
                contentType = pagedResults.itemContentType { "episode" }
            ) { index ->
                val episode = pagedResults[index]
                if (episode != null) {
                    EpisodeListItem(
                        episode = episode,
                        podcastTitle = episode.podcastTitle,
                        isBuffering = playerState.currentEpisode?.id == episode.id && playerState.isBuffering,
                        isPlaying = playerState.currentEpisode?.id == episode.id && playerState.isPlaying,
                        downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
                        onClick = { onEpisodeClick(episode.id, episode.podcastId) },
                        onPlayClick = { viewModel.playEpisode(episode) },
                        onDownloadClick = { viewModel.downloadEpisode(episode) },
                        onDeleteClick = { viewModel.showDeleteConfirmation(episode) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            if (pagedResults.loadState.append is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
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
