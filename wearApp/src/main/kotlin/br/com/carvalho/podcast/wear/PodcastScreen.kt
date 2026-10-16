package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.download_cd
import br.com.carvalho.podcast.core.ui.generated.resources.episode_downloading
import br.com.carvalho.podcast.core.ui.generated.resources.episode_played
import br.com.carvalho.podcast.core.ui.generated.resources.episode_playing
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.remaining_time
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailIntent
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailViewModel
import br.com.carvalho.podcast.presentation.component.remainingDuration
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * A podcast's episodes, newest first unless the podcast says otherwise. A tap plays one and opens the player; a long
 * press downloads it, to hear it away from Wi-Fi and LTE.
 */
@Composable
fun PodcastScreen(
    podcastId: String,
    onPlaying: () -> Unit,
    viewModel: PodcastDetailViewModel = koinViewModel(key = podcastId) { parametersOf(podcastId) },
) {
    val state by viewModel.uiState.collectAsState()
    val player by viewModel.playerState.collectAsState()
    val downloads by viewModel.activeDownloads.collectAsState()
    val episodes = viewModel.pagedEpisodes.collectAsLazyPagingItems()
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(state.podcast?.title.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis) } }
            items(episodes.itemCount) { index ->
                episodes[index]?.let { episode ->
                    EpisodeButton(
                        episode,
                        isPlaying = player.isPlaying && player.currentEpisode?.id == episode.id,
                        download = downloads[episode.id],
                        onPlay = {
                            viewModel.onIntent(PodcastDetailIntent.Play(episode))
                            onPlaying()
                        },
                        onDownload = { viewModel.onIntent(PodcastDetailIntent.Download(episode)) },
                    )
                }
            }
        }
    }
}

/**
 * An episode and where it stands (19.9g): playing, downloading with its progress, heard, or how much is left; then
 * whether it is on the watch. The icon is the episode's cover unless it is playing or downloading.
 */
@Composable
internal fun EpisodeButton(
    episode: Episode,
    isPlaying: Boolean,
    download: DownloadStatus?,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    val progressSize = 32.dp
    val isDownloading = download is DownloadStatus.Queued || download is DownloadStatus.Downloading
    val remaining = episode.remainingDuration()
    val status = when {
        isPlaying -> stringResource(Res.string.episode_playing)
        isDownloading -> stringResource(Res.string.episode_downloading)
        episode.isPlayed -> stringResource(Res.string.episode_played)
        remaining != null -> stringResource(Res.string.remaining_time, remaining)
        else -> episode.duration.takeIf { it > 0 }?.toDuration()
    }
    val onWatch = stringResource(Res.string.filter_downloaded).takeIf { episode.isDownloaded && !isDownloading }
    Button(
        onClick = onPlay,
        onLongClick = onDownload,
        onLongClickLabel = stringResource(Res.string.download_cd),
        icon = {
            when {
                isPlaying -> Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                download is DownloadStatus.Downloading ->
                    CircularProgressIndicator(progress = { download.progress }, modifier = Modifier.size(progressSize))
                isDownloading -> CircularProgressIndicator(modifier = Modifier.size(progressSize))
                else -> Thumbnail(episode.imageUrl)
            }
        },
        label = { Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = listOfNotNull(status, onWatch).joinToString(" · ").ifEmpty { null }?.let { { Text(it) } },
        // A heard episode is outlined, so what is left to hear stands out.
        colors = if (episode.isPlayed) {
            ButtonDefaults.outlinedButtonColors()
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
