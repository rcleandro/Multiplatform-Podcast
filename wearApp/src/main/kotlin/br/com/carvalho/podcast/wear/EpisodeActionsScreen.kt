package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add_to_queue
import br.com.carvalho.podcast.core.ui.generated.resources.cancel_download
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download
import br.com.carvalho.podcast.core.ui.generated.resources.download_cd
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.play_next
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailIntent
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailUiState
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * What can be done with an episode (19.9f), opened by a long press in any list: play or pause, play next or add to
 * the queue, download (or cancel or delete it) and mark it heard or not. Each choice goes back to the list.
 */
@Composable
fun EpisodeActionsScreen(
    episodeId: String,
    onDone: () -> Unit,
    viewModel: EpisodeDetailViewModel = koinViewModel(key = episodeId) { parametersOf(episodeId) },
) {
    val state by viewModel.uiState.collectAsState()
    EpisodeActionsContent(state) { intent ->
        viewModel.onIntent(intent)
        onDone()
    }
}

@Composable
fun EpisodeActionsContent(state: EpisodeDetailUiState, onAction: (EpisodeDetailIntent) -> Unit) {
    val episode = state.episode ?: return
    val listState = rememberScalingLazyListState()
    val download = when (state.downloadStatus) {
        is DownloadStatus.Queued, is DownloadStatus.Downloading ->
            Triple(Icons.Rounded.Close, Res.string.cancel_download, EpisodeDetailIntent.CancelDownload)
        else -> if (episode.isDownloaded) {
            Triple(Icons.Rounded.Delete, Res.string.delete_download, EpisodeDetailIntent.DeleteDownload)
        } else {
            Triple(Icons.Rounded.Download, Res.string.download_cd, EpisodeDetailIntent.Download)
        }
    }
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { Text(episode.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center) }
            item {
                ActionButton(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (state.isPlaying) Res.string.pause else Res.string.play),
                    primary = true,
                ) { onAction(EpisodeDetailIntent.PlayPause) }
            }
            item {
                ActionButton(Icons.Rounded.QueuePlayNext, stringResource(Res.string.play_next)) {
                    onAction(EpisodeDetailIntent.Enqueue(next = true))
                }
            }
            item {
                ActionButton(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(Res.string.add_to_queue)) {
                    onAction(EpisodeDetailIntent.Enqueue(next = false))
                }
            }
            item { ActionButton(download.first, stringResource(download.second)) { onAction(download.third) } }
            item {
                if (episode.isPlayed) {
                    ActionButton(Icons.Rounded.RemoveDone, stringResource(Res.string.mark_as_unplayed)) {
                        onAction(EpisodeDetailIntent.MarkUnplayed)
                    }
                } else {
                    ActionButton(Icons.Rounded.Done, stringResource(Res.string.mark_as_played)) {
                        onAction(EpisodeDetailIntent.MarkPlayed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, primary: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        colors = if (primary) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
