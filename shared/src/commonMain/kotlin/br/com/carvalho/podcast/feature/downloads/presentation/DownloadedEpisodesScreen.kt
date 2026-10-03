package br.com.carvalho.podcast.feature.downloads.presentation

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.cancel
import br.com.carvalho.podcast.shared.delete
import br.com.carvalho.podcast.shared.delete_download
import br.com.carvalho.podcast.shared.delete_download_confirmation
import br.com.carvalho.podcast.shared.downloads
import br.com.carvalho.podcast.shared.downloads_empty_message
import br.com.carvalho.podcast.shared.no_downloads
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DownloadedEpisodesScreen(
    viewModel: DownloadedEpisodesViewModel = koinViewModel(),
    onEpisodeClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(getString(it))
            viewModel.clearSnackbarMessage()
        }
    }

    DownloadedEpisodesContent(
        state = uiState,
        playerState = playerState,
        activeDownloads = activeDownloads,
        snackbarHostState = snackbarHostState,
        onEpisodeClick = { onEpisodeClick(it.id, it.podcastId) },
        onPlay = viewModel::playEpisode,
        onRemove = viewModel::showDeleteConfirmation,
        onConfirmRemove = { viewModel.deleteDownload(it.id) },
        onDismissRemove = viewModel::hideDeleteConfirmation,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedEpisodesContent(
    state: DownloadedEpisodesUiState,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    onEpisodeClick: (Episode) -> Unit,
    onPlay: (Episode) -> Unit,
    onRemove: (Episode) -> Unit,
    onConfirmRemove: (Episode) -> Unit,
    onDismissRemove: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { DownloadsTopBar(scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets()
    ) { padding ->
        if (state.episodes.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.DownloadDone,
                title = stringResource(Res.string.no_downloads),
                message = stringResource(Res.string.downloads_empty_message),
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = Sizes.listBottomInset)
            ) {
                items(state.episodes, key = { it.id }) { episode ->
                    val isCurrent = playerState.currentEpisode?.id == episode.id
                    EpisodeListItem(
                        episode = episode,
                        podcastTitle = episode.podcastTitle,
                        isBuffering = isCurrent && playerState.isBuffering,
                        isPlaying = isCurrent && playerState.isPlaying,
                        downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Completed(""),
                        onClick = { onEpisodeClick(episode) },
                        onPlayClick = { onPlay(episode) },
                        onDeleteClick = { onRemove(episode) }
                    )
                }
            }
        }

        state.deleteEpisodeConfirmation?.let { episode ->
            ConfirmDialog(
                title = stringResource(Res.string.delete_download),
                message = stringResource(Res.string.delete_download_confirmation, episode.title),
                confirmLabel = stringResource(Res.string.delete),
                dismissLabel = stringResource(Res.string.cancel),
                onConfirm = { onConfirmRemove(episode) },
                onDismiss = onDismissRemove,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsTopBar(scrollBehavior: TopAppBarScrollBehavior) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.downloads),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior
    )
}
