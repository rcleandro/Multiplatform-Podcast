package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import br.com.carvalho.podcast.core.designsystem.component.PodcastSnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.ui.unit.Dp
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_episode_count
import br.com.carvalho.podcast.core.ui.generated.resources.play_latest
import org.jetbrains.compose.resources.pluralStringResource
import br.com.carvalho.podcast.core.designsystem.component.ArtworkBackdrop
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.designsystem.component.FilterChipRow
import br.com.carvalho.podcast.core.designsystem.component.FilterOption
import br.com.carvalho.podcast.core.designsystem.component.HtmlText
import br.com.carvalho.podcast.core.designsystem.component.LoadingState
import br.com.carvalho.podcast.core.designsystem.component.PodcastArtwork
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.presentation.MessageEffect
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.presentation.component.MarkOlderDialog
import br.com.carvalho.podcast.presentation.component.OlderMark
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.filter_all
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.filter_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.show_less
import br.com.carvalho.podcast.core.ui.generated.resources.show_more
import br.com.carvalho.podcast.core.ui.generated.resources.refresh
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PodcastDetailScreen(
    podcastId: String,
    viewModel: PodcastDetailViewModel = koinViewModel(key = podcastId) { parametersOf(podcastId) },
    onBackClick: () -> Unit,
    onEpisodeClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedEpisodes = viewModel.pagedEpisodes.collectAsLazyPagingItems()
    val playerState by viewModel.playerState.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    MessageEffect(viewModel.messages, snackbarHostState)

    PodcastDetailContent(
        state = uiState,
        episodes = pagedEpisodes,
        playerState = playerState,
        activeDownloads = activeDownloads,
        snackbarHostState = snackbarHostState,
        actions = PodcastDetailActions(
            onBack = onBackClick,
            onRefresh = { viewModel.onIntent(PodcastDetailIntent.Refresh) },
            onFilterSelected = { viewModel.onIntent(PodcastDetailIntent.SetFilter(it)) },
            onEpisodeClick = { onEpisodeClick(it.id, it.podcastId) },
            onRequestMarkOlder = { viewModel.onIntent(PodcastDetailIntent.RequestMarkOlder(it)) },
            onPlay = { viewModel.onIntent(PodcastDetailIntent.Play(it)) },
            onDownload = { viewModel.onIntent(PodcastDetailIntent.Download(it)) },
            onCancelDownload = { viewModel.onIntent(PodcastDetailIntent.CancelDownload(it)) },
            onRemoveDownload = { viewModel.onIntent(PodcastDetailIntent.RequestDeleteDownload(it)) },
            onConfirmRemoveDownload = { viewModel.onIntent(PodcastDetailIntent.ConfirmDeleteDownload(it)) },
            onDismissRemoveDownload = { viewModel.onIntent(PodcastDetailIntent.DismissDeleteDownload) },
            onMarkPlayed = { viewModel.onIntent(PodcastDetailIntent.MarkPlayed(it)) },
            onMarkUnplayed = { viewModel.onIntent(PodcastDetailIntent.MarkUnplayed(it)) },
            onConfirmMarkOlder = { viewModel.onIntent(PodcastDetailIntent.ConfirmMarkOlder(it)) },
            onDismissMarkOlder = { viewModel.onIntent(PodcastDetailIntent.DismissMarkOlder) },
        ),
    )
}

data class PodcastDetailActions(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onFilterSelected: (EpisodeFilter) -> Unit = {},
    val onEpisodeClick: (Episode) -> Unit = {},
    val onRequestMarkOlder: (OlderMark) -> Unit = {},
    val onPlay: (Episode) -> Unit = {},
    val onDownload: (Episode) -> Unit = {},
    val onCancelDownload: (Episode) -> Unit = {},
    val onRemoveDownload: (Episode) -> Unit = {},
    val onConfirmRemoveDownload: (Episode) -> Unit = {},
    val onDismissRemoveDownload: () -> Unit = {},
    val onMarkPlayed: (Episode) -> Unit = {},
    val onMarkUnplayed: (Episode) -> Unit = {},
    val onConfirmMarkOlder: (OlderMark) -> Unit = {},
    val onDismissMarkOlder: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailContent(
    state: PodcastDetailUiState,
    episodes: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: PodcastDetailActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    // The header shows the name; once it scrolls away, the bar takes it over.
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { PodcastSnackbarHost(snackbarHostState) },
        topBar = { PodcastDetailTopBar(scrollBehavior, state.podcast?.title?.takeIf { headerGone }, actions) },
        // The tinted header runs behind the bar, which stays clear until the header scrolls away.
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets()
    ) { padding ->
        val topInset = padding.calculateTopPadding()
        val refreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = actions.onRefresh,
            state = refreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = refreshState,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = topInset),
                )
            },
        ) {
            if (state.isLoading) {
                LoadingState(modifier = Modifier.padding(top = topInset))
            } else {
                EpisodeList(state, episodes, playerState, activeDownloads, actions, listState, topInset)
            }
            PodcastDetailDialogs(state, actions)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastDetailTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String?,
    actions: PodcastDetailActions,
) {
    TopAppBar(
        title = { title?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        navigationIcon = {
            IconButton(onClick = actions.onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = if (title == null) 0f else 1f),
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun EpisodeList(
    state: PodcastDetailUiState,
    episodes: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: PodcastDetailActions,
    listState: LazyListState,
    topInset: Dp,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = if (state.podcast == null) topInset else 0.dp,
            bottom = Sizes.listBottomInset,
        ),
    ) {
        state.podcast?.let { podcast -> item { PodcastHeader(podcast, state, actions, topInset) } }
        item {
            FilterChipRow(
                // "Downloaded" stays last, so the indices still match EpisodeFilter where it is left out.
                options = listOfNotNull(
                    FilterOption(stringResource(Res.string.filter_all)),
                    FilterOption(stringResource(Res.string.filter_unplayed)),
                    FilterOption(stringResource(Res.string.filter_downloaded)).takeIf { supportsDownloads },
                ),
                selectedIndex = state.filter.ordinal,
                onSelected = { actions.onFilterSelected(EpisodeFilter.entries[it]) },
                modifier = Modifier.readableWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
            )
        }
        items(
            count = episodes.itemCount,
            key = episodes.itemKey { it.id },
            contentType = episodes.itemContentType { "episode" }
        ) { index ->
            episodes[index]?.let { episode ->
                val isCurrent = playerState.currentEpisode?.id == episode.id
                EpisodeListItem(
                    episode = episode,
                    modifier = Modifier.readableWidth(),
                    isBuffering = isCurrent && playerState.isBuffering,
                    isPlaying = isCurrent && playerState.isPlaying,
                    downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
                    onClick = { actions.onEpisodeClick(episode) },
                    onPlayClick = { actions.onPlay(episode) },
                    onMarkPlayed = { actions.onMarkPlayed(episode) },
                    onMarkUnplayed = { actions.onMarkUnplayed(episode) },
                    onMarkOlderPlayed = { actions.onRequestMarkOlder(OlderMark(episode, played = true)) },
                    onMarkOlderUnplayed = { actions.onRequestMarkOlder(OlderMark(episode, played = false)) },
                    onDownloadClick = { actions.onDownload(episode) },
                    onCancelDownloadClick = { actions.onCancelDownload(episode) },
                    onDeleteClick = { actions.onRemoveDownload(episode) }
                )
            }
        }
    }
}

@Composable
private fun PodcastDetailDialogs(state: PodcastDetailUiState, actions: PodcastDetailActions) {
    state.olderMark?.let { MarkOlderDialog(it, actions.onConfirmMarkOlder, actions.onDismissMarkOlder) }
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

/**
 * The cover centered over a background tinted by it (24.2), the name, "author · 312 episodes", and the two things
 * to do here: play the newest unheard episode, or look for new ones.
 */
@Composable
private fun PodcastHeader(
    podcast: Podcast,
    state: PodcastDetailUiState,
    actions: PodcastDetailActions,
    topInset: Dp,
) {
    val artworkSize = 168.dp
    ArtworkBackdrop(imageUrl = podcast.imageUrl, modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier
                .readableWidth()
                .padding(horizontal = Spacing.l)
                .padding(top = topInset + Spacing.m, bottom = Spacing.m),
        ) {
            PodcastArtwork(
                imageUrl = podcast.imageUrl,
                contentDescription = null,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.size(artworkSize),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
                val count = state.episodeCount.takeIf { it > 0 }
                    ?.let { pluralStringResource(Res.plurals.podcast_episode_count, it, it) }
                listOfNotNull(podcast.author, count).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            HeaderActions(state, actions)
            CollapsibleDescription(podcast.description)
        }
    }
}

@Composable
private fun HeaderActions(state: PodcastDetailUiState, actions: PodcastDetailActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        state.latestUnplayed?.let { latest ->
            Button(onClick = { actions.onPlay(latest) }) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(Spacing.s))
                Text(stringResource(Res.string.play_latest))
            }
        }
        FilledTonalIconButton(onClick = actions.onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(Res.string.refresh))
        }
    }
}

/** A long description shows its first lines and "Show more"; a short one shows whole, with no button. */
@Composable
private fun CollapsibleDescription(html: String) {
    var expanded by rememberSaveable(html) { mutableStateOf(false) }
    var overflows by remember(html) { mutableStateOf(false) }
    Column {
        HtmlText(
            html = html,
            maxLines = if (expanded) Int.MAX_VALUE else DESCRIPTION_COLLAPSED_LINES,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            modifier = Modifier.fillMaxWidth(),
        )
        if (overflows || expanded) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(if (expanded) Res.string.show_less else Res.string.show_more))
            }
        }
    }
}

private const val TITLE_MAX_LINES = 3
private const val DESCRIPTION_COLLAPSED_LINES = 4
