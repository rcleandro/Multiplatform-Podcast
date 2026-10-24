package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.designsystem.icons.automirrored.PlaylistPlay
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.filter_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.no_downloads
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_length
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_new
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_play_all
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlists
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.SmartPlaylist
import br.com.carvalho.podcast.feature.search.presentation.SearchIntent
import br.com.carvalho.podcast.feature.search.presentation.SearchViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * A titled list of episodes, each with its state (19.9g): a tap plays it, a long press opens its actions. [top] goes
 * between the title and the episodes; [empty] shows when there are none.
 */
@Composable
internal fun EpisodeList(
    title: String,
    episodes: LazyPagingItems<Episode>,
    player: PlayerState,
    downloads: Map<String, DownloadStatus>,
    onPlay: (Episode) -> Unit,
    onActions: (Episode) -> Unit,
    empty: String? = null,
    top: ScalingLazyListScope.() -> Unit = {},
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
            top()
            if (episodes.itemCount == 0 && empty != null) item { Text(empty) }
            items(episodes.itemCount) { index ->
                episodes[index]?.let { episode ->
                    EpisodeButton(
                        episode,
                        isPlaying = player.isPlaying && player.currentEpisode?.id == episode.id,
                        download = downloads[episode.id],
                        onPlay = { onPlay(episode) },
                        onActions = { onActions(episode) },
                    )
                }
            }
        }
    }
}

/** What is on the watch (19.9f): the episodes heard without a network, newest first. */
@Composable
fun DownloadsScreen(
    onPlaying: () -> Unit,
    onActions: (Episode) -> Unit,
    viewModel: SearchViewModel = koinViewModel(key = DOWNLOADS_KEY),
) {
    LaunchedEffect(viewModel) { viewModel.onIntent(SearchIntent.ChangeFilter(EpisodeListFilter.DOWNLOADED)) }
    val player by viewModel.playerState.collectAsState()
    val downloads by viewModel.activeDownloads.collectAsState()
    EpisodeList(
        title = stringResource(Res.string.filter_downloaded),
        episodes = viewModel.pagedResults.collectAsLazyPagingItems(),
        player = player,
        downloads = downloads,
        onPlay = {
            viewModel.onIntent(SearchIntent.Play(it))
            onPlaying()
        },
        onActions = onActions,
        empty = stringResource(Res.string.no_downloads),
    )
}

/**
 * The smart playlists (19.4, 19.9f). The watch has no keyboard to name one, so the usual ones are a tap away under
 * "New playlist" until made; the phone's arrive with the sync (19.9b) or a restored backup.
 */
@Composable
fun PlaylistsScreen(onPlaylist: (Long) -> Unit, viewModel: SearchViewModel = koinViewModel(key = PLAYLISTS_KEY)) {
    val state by viewModel.uiState.collectAsState()
    val presets = playlistPresets()
    PlaylistsContent(
        playlists = state.playlists,
        presets = presets.filter { preset -> state.playlists.none { it.name == preset.name } },
        onPlaylist = onPlaylist,
        onCreate = { viewModel.onIntent(SearchIntent.SavePlaylist(it)) },
    )
}

/** Unheard episodes, and the unheard ones up to 15 and 30 minutes, named with the words the filters already use. */
@Composable
private fun playlistPresets(): List<SmartPlaylist> {
    val (short, medium) = SmartPlaylist.LENGTH_CHOICES
    val upToShort = stringResource(Res.string.smart_playlist_length, short)
    val upToMedium = stringResource(Res.string.smart_playlist_length, medium)
    return listOf(
        SmartPlaylist(name = stringResource(Res.string.filter_unplayed), unplayedOnly = true),
        SmartPlaylist(name = upToShort, unplayedOnly = true, maxMinutes = short),
        SmartPlaylist(name = upToMedium, unplayedOnly = true, maxMinutes = medium),
    )
}

@Composable
fun PlaylistsContent(
    playlists: List<SmartPlaylist>,
    onPlaylist: (Long) -> Unit,
    presets: List<SmartPlaylist> = emptyList(),
    onCreate: (SmartPlaylist) -> Unit = {},
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(Res.string.smart_playlists)) } }
            items(playlists, key = { it.id }) { playlist ->
                Button(
                    onClick = { onPlaylist(playlist.id) },
                    icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
                    label = { Text(playlist.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (presets.isNotEmpty()) {
                item { ListHeader { Text("+ " + stringResource(Res.string.smart_playlist_new)) } }
            }
            items(presets, key = { "preset-${it.name}" }) { preset ->
                OutlinedButton(
                    onClick = { onCreate(preset) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    label = { Text(preset.name) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** A smart playlist's episodes, and "Play all" above them. */
@Composable
fun PlaylistScreen(
    playlistId: Long,
    onPlaying: () -> Unit,
    onActions: (Episode) -> Unit,
    viewModel: SearchViewModel = koinViewModel(key = "$PLAYLISTS_KEY-$playlistId"),
) {
    val state by viewModel.uiState.collectAsState()
    val player by viewModel.playerState.collectAsState()
    val downloads by viewModel.activeDownloads.collectAsState()
    val playlist = state.playlists.find { it.id == playlistId }
    LaunchedEffect(playlist) { playlist?.let { viewModel.onIntent(SearchIntent.SelectPlaylist(it)) } }
    val episodes = viewModel.pagedResults.collectAsLazyPagingItems()
    EpisodeList(
        title = playlist?.name.orEmpty(),
        episodes = episodes,
        player = player,
        downloads = downloads,
        onPlay = {
            viewModel.onIntent(SearchIntent.Play(it))
            onPlaying()
        },
        onActions = onActions,
        empty = stringResource(Res.string.smart_playlist_empty_title),
    ) {
        if (episodes.itemCount > 0) {
            item {
                Button(
                    onClick = {
                        viewModel.onIntent(SearchIntent.PlayPlaylist)
                        onPlaying()
                    },
                    icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
                    label = { Text(stringResource(Res.string.smart_playlist_play_all)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private const val DOWNLOADS_KEY = "wear-downloads"
private const val PLAYLISTS_KEY = "wear-playlists"
