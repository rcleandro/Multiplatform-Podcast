package br.com.carvalho.podcast.feature.downloads.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.presentation.component.EpisodeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedEpisodesScreen(
    viewModel: DownloadedEpisodesViewModel,
    onEpisodeClick: (String, String) -> Unit
) {
    val episodes by viewModel.episodes.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets()
    ) { padding ->
        if (episodes.isEmpty()) {
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(episodes, key = { it.id }) { episode ->
                    EpisodeListItem(
                        episode = episode,
                        podcastTitle = episode.podcastTitle,
                        isPlaying = playerState.currentEpisode?.id == episode.id && playerState.isPlaying,
                        downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Completed(""),
                        onClick = { onEpisodeClick(episode.id, episode.podcastId) },
                        onPlayClick = { viewModel.playEpisode(episode) },
                        onDeleteClick = { viewModel.deleteDownload(episode.id) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
