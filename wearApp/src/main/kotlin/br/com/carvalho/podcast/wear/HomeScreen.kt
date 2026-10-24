package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.designsystem.icons.DownloadDone
import br.com.carvalho.podcast.core.designsystem.icons.TrendingUp
import br.com.carvalho.podcast.core.designsystem.icons.automirrored.PlaylistPlay
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.library_title
import br.com.carvalho.podcast.core.ui.generated.resources.now_playing
import br.com.carvalho.podcast.core.ui.generated.resources.popular
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlists
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.feature.library.presentation.LibraryViewModel
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.presentation.selectAsState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** What plays now, the popular podcasts to subscribe to, and the library's podcasts. */
@Composable
fun HomeScreen(
    onPlayer: () -> Unit,
    onPopular: () -> Unit,
    onPodcast: (String) -> Unit,
    onDownloads: () -> Unit = {},
    onPlaylists: () -> Unit = {},
    libraryViewModel: LibraryViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
) {
    val library by libraryViewModel.uiState.collectAsState()
    val playing by playerViewModel.playerState.selectAsState { it.currentEpisode }
    HomeContent(library.podcasts, playing, onPlayer, onPopular, onPodcast, onDownloads, onPlaylists)
}

@Composable
fun HomeContent(
    podcasts: List<LibraryEntry>,
    playing: Episode?,
    onPlayer: () -> Unit,
    onPopular: () -> Unit,
    onPodcast: (String) -> Unit,
    onDownloads: () -> Unit = {},
    onPlaylists: () -> Unit = {},
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(Res.string.library_title)) } }
            playing?.let { episode ->
                item {
                    Button(
                        onClick = onPlayer,
                        icon = { Thumbnail(episode.imageUrl) },
                        label = { Text(stringResource(Res.string.now_playing)) },
                        secondaryLabel = { Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                FilledTonalButton(
                    onClick = onPopular,
                    icon = { Icon(Icons.Rounded.TrendingUp, contentDescription = null) },
                    label = { Text(stringResource(Res.string.popular)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Shortcut(Icons.Rounded.DownloadDone, stringResource(Res.string.filter_downloaded), onDownloads) }
            item {
                val playlists = stringResource(Res.string.smart_playlists)
                Shortcut(Icons.AutoMirrored.Rounded.PlaylistPlay, playlists, onPlaylists)
            }
            items(podcasts, key = { it.podcast.id }) { entry ->
                Button(
                    onClick = { onPodcast(entry.podcast.id) },
                    icon = { Thumbnail(entry.podcast.imageUrl) },
                    label = { Text(entry.podcast.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** One of the home's shortcuts: what is on the watch, the smart playlists (19.9f). */
@Composable
private fun Shortcut(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}
