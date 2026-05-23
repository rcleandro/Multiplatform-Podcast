package br.com.carvalho.podcast.feature.downloads.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedEpisodesScreen(
    viewModel: DownloadedEpisodesViewModel = koinViewModel(),
    onEpisodeClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets()
    ) { padding ->
        if (uiState.episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum episódio baixado",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.episodes, key = { it.id }) { episode ->
                EpisodeListItem(
                    episode = episode,
                    podcastTitle = episode.podcastTitle,
                    isBuffering = playerState.currentEpisode?.id == episode.id && playerState.isBuffering,
                    isPlaying = playerState.currentEpisode?.id == episode.id && playerState.isPlaying,
                    downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Completed(""),
                    onClick = { onEpisodeClick(episode.id, episode.podcastId) },
                    onPlayClick = { viewModel.playEpisode(episode) },
                    onDeleteClick = { viewModel.showDeleteConfirmation(episode) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
