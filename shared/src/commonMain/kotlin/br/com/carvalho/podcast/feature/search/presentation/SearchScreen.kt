package br.com.carvalho.podcast.feature.search.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.presentation.component.EpisodeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onEpisodeClick: (String, String) -> Unit,
    onPlayEpisode: (Episode) -> Unit
) {
    val pagedResults = viewModel.pagedResults.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val error by viewModel.error.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val playerState by viewModel.audioPlayer.playerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(error) {
        error?.let {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            placeholder = { Text("Buscar episódios...") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
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
                }
            )
        },
        contentWindowInsets = WindowInsets()
    ) { padding ->
        val refreshState = pagedResults.loadState.refresh

        if (refreshState is LoadState.Loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (pagedResults.itemCount == 0 && searchQuery.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhum resultado para \"$searchQuery\"")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
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
                            isPlaying = playerState.currentEpisode?.id == episode.id && playerState.isPlaying,
                            downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
                            onClick = { onEpisodeClick(episode.id, episode.podcastId) },
                            onPlayClick = { viewModel.playEpisode(episode) },
                            onDownloadClick = { viewModel.downloadEpisode(episode) },
                            onDeleteClick = { viewModel.deleteDownload(episode.id) }
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
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}
