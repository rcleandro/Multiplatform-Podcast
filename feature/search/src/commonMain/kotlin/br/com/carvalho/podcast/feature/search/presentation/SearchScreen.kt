package br.com.carvalho.podcast.feature.search.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.core.designsystem.component.ErrorState
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.presentation.MessageEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.clear
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.error_loading_results
import br.com.carvalho.podcast.core.ui.generated.resources.search
import br.com.carvalho.podcast.core.ui.generated.resources.search_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.search_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.search_placeholder
import br.com.carvalho.podcast.core.ui.generated.resources.try_again
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = koinViewModel(),
    onEpisodeClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedResults = viewModel.pagedResults.collectAsLazyPagingItems()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onIntent(SearchIntent.Refresh) }
    MessageEffect(viewModel.messages, snackbarHostState)

    SearchContent(
        state = uiState,
        results = pagedResults,
        playerState = playerState,
        activeDownloads = activeDownloads,
        actions = SearchActions(
            onQueryChange = { viewModel.onIntent(SearchIntent.ChangeQuery(it)) },
            onEpisodeClick = { onEpisodeClick(it.id, it.podcastId) },
            onPlay = { viewModel.onIntent(SearchIntent.Play(it)) },
            onDownload = { viewModel.onIntent(SearchIntent.Download(it)) },
            onCancelDownload = { viewModel.onIntent(SearchIntent.CancelDownload(it)) },
            onRemoveDownload = { viewModel.onIntent(SearchIntent.RequestDeleteDownload(it)) },
            onConfirmRemoveDownload = { viewModel.onIntent(SearchIntent.ConfirmDeleteDownload(it)) },
            onDismissRemoveDownload = { viewModel.onIntent(SearchIntent.DismissDeleteDownload) },
        ),
        snackbarHostState = snackbarHostState,
    )
}

data class SearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onEpisodeClick: (Episode) -> Unit = {},
    val onPlay: (Episode) -> Unit = {},
    val onDownload: (Episode) -> Unit = {},
    val onCancelDownload: (Episode) -> Unit = {},
    val onRemoveDownload: (Episode) -> Unit = {},
    val onConfirmRemoveDownload: (Episode) -> Unit = {},
    val onDismissRemoveDownload: () -> Unit = {},
)

@Composable
fun SearchContent(
    state: SearchUiState,
    results: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: SearchActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = { SearchField(query = state.searchQuery, onQueryChange = actions.onQueryChange) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets()
    ) { padding ->
        val refresh = results.loadState.refresh
        when {
            results.itemCount == 0 && refresh is LoadState.Error -> ErrorState(
                icon = Icons.Rounded.CloudOff,
                title = stringResource(Res.string.error_loading_results),
                message = null,
                actionLabel = stringResource(Res.string.try_again),
                onAction = results::retry,
                modifier = Modifier.padding(padding),
            )
            results.itemCount == 0 && refresh is LoadState.NotLoading -> EmptyState(
                icon = Icons.Rounded.Search,
                title = stringResource(Res.string.search_empty_title),
                message = stringResource(Res.string.search_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> SearchResults(results, playerState, activeDownloads, actions, Modifier.padding(padding))
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
}

@Composable
private fun SearchResults(
    results: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: SearchActions,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Sizes.listBottomInset)
    ) {
        items(
            count = results.itemCount,
            key = results.itemKey { it.id },
            contentType = results.itemContentType { "episode" }
        ) { index ->
            results[index]?.let { episode ->
                val isCurrent = playerState.currentEpisode?.id == episode.id
                EpisodeListItem(
                    episode = episode,
                    podcastTitle = episode.podcastTitle,
                    isBuffering = isCurrent && playerState.isBuffering,
                    isPlaying = isCurrent && playerState.isPlaying,
                    downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
                    onClick = { actions.onEpisodeClick(episode) },
                    onPlayClick = { actions.onPlay(episode) },
                    onDownloadClick = { actions.onDownload(episode) },
                    onCancelDownloadClick = { actions.onCancelDownload(episode) },
                    onDeleteClick = { actions.onRemoveDownload(episode) }
                )
            }
        }
        if (results.loadState.append is LoadState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.l), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/** Pill-shaped search field; the indicator line takes the container color so it does not show. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val container = MaterialTheme.colorScheme.surfaceContainer
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(stringResource(Res.string.search_placeholder), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = stringResource(Res.string.search)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(Res.string.clear))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = container,
            unfocusedIndicatorColor = container,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
    )
}
