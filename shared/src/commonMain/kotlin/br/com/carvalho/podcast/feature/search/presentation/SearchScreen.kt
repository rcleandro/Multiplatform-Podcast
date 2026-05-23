package br.com.carvalho.podcast.feature.search.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import br.com.carvalho.podcast.core.designsystem.AppDimensions
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.cancel
import br.com.carvalho.podcast.shared.clear
import br.com.carvalho.podcast.shared.delete
import br.com.carvalho.podcast.shared.delete_download
import br.com.carvalho.podcast.shared.delete_download_confirmation
import br.com.carvalho.podcast.shared.error_loading_results
import br.com.carvalho.podcast.shared.refresh
import br.com.carvalho.podcast.shared.search
import br.com.carvalho.podcast.shared.search_placeholder
import org.jetbrains.compose.resources.stringResource
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

    val errorLoadingResults = stringResource(Res.string.error_loading_results)
    LaunchedEffect(pagedResults.loadState.refresh) {
        val refreshState = pagedResults.loadState.refresh
        if (refreshState is LoadState.Error) {
            viewModel.setError(errorLoadingResults)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(end = AppDimensions.paddingNormal),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            placeholder = {
                                Text(
                                    text = stringResource(Res.string.search_placeholder),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = stringResource(Res.string.search)
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(
                                            Icons.Rounded.Clear,
                                            contentDescription = stringResource(Res.string.clear)
                                        )
                                    }
                                }
                            },
                            singleLine = true
                        )
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = stringResource(Res.string.refresh)
                            )
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
            contentPadding = PaddingValues(bottom = AppDimensions.miniPlayerHeightWithPadding)
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
                    HorizontalDivider(modifier = Modifier.padding(horizontal = AppDimensions.paddingNormal))
                }
            }

            if (pagedResults.loadState.append is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(AppDimensions.paddingNormal),
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
