package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.download_cd
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailIntent
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailViewModel
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
    val episodes = viewModel.pagedEpisodes.collectAsLazyPagingItems()
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(state.podcast?.title.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis) } }
            items(episodes.itemCount) { index ->
                episodes[index]?.let { episode ->
                    EpisodeButton(
                        episode,
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

@Composable
private fun EpisodeButton(episode: Episode, onPlay: () -> Unit, onDownload: () -> Unit) {
    val length = episode.duration.takeIf { it > 0 }?.toDuration()
    val downloaded = stringResource(Res.string.filter_downloaded).takeIf { episode.isDownloaded }
    Button(
        onClick = onPlay,
        onLongClick = onDownload,
        onLongClickLabel = stringResource(Res.string.download_cd),
        label = { Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = listOfNotNull(length, downloaded).joinToString(" · ").ifEmpty { null }?.let { { Text(it) } },
        // A heard episode is outlined, so what is left to hear stands out.
        colors = if (episode.isPlayed) {
            ButtonDefaults.outlinedButtonColors()
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
