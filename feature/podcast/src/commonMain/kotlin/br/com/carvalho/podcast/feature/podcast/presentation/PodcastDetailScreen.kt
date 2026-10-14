package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ArtworkBackdrop
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.designsystem.component.FilterChipRow
import br.com.carvalho.podcast.core.designsystem.component.FilterOption
import br.com.carvalho.podcast.core.designsystem.component.HtmlText
import br.com.carvalho.podcast.core.designsystem.component.LoadingState
import br.com.carvalho.podcast.core.designsystem.component.PodcastArtwork
import br.com.carvalho.podcast.core.designsystem.component.PodcastSnackbarHost
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.episodes_newest_first
import br.com.carvalho.podcast.core.ui.generated.resources.episodes_oldest_first
import br.com.carvalho.podcast.core.ui.generated.resources.filter_all
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.filter_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort
import br.com.carvalho.podcast.core.ui.generated.resources.play_latest
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_episode_count
import br.com.carvalho.podcast.core.ui.generated.resources.refresh
import br.com.carvalho.podcast.core.ui.generated.resources.season_number
import br.com.carvalho.podcast.core.ui.generated.resources.share
import br.com.carvalho.podcast.core.ui.generated.resources.show_less
import br.com.carvalho.podcast.core.ui.generated.resources.show_more
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.usecase.EpisodeAction
import br.com.carvalho.podcast.presentation.MessageEffect
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.presentation.component.EpisodeSelectionBar
import br.com.carvalho.podcast.presentation.component.MarkOlderDialog
import br.com.carvalho.podcast.presentation.component.OlderMark
import br.com.carvalho.podcast.presentation.share.podcastShareText
import br.com.carvalho.podcast.presentation.share.rememberShare
import org.jetbrains.compose.resources.pluralStringResource
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
    val share = rememberShare(snackbarHostState)

    PodcastDetailContent(
        state = uiState,
        episodes = pagedEpisodes,
        playerState = playerState,
        activeDownloads = activeDownloads,
        snackbarHostState = snackbarHostState,
        actions = PodcastDetailActions(
            onBack = onBackClick,
            onShare = { uiState.podcast?.let { share(podcastShareText(it)) } },
            onRefresh = { viewModel.onIntent(PodcastDetailIntent.Refresh) },
            onFilterSelected = { viewModel.onIntent(PodcastDetailIntent.SetFilter(it)) },
            onOldestFirstChange = { viewModel.onIntent(PodcastDetailIntent.SetOldestFirst(it)) },
            onEpisodeClick = { onEpisodeClick(it.id, it.podcastId) },
            onRequestMarkOlder = { viewModel.onIntent(PodcastDetailIntent.RequestMarkOlder(it)) },
            onPlay = { viewModel.onIntent(PodcastDetailIntent.Play(it)) },
            onEnqueue = { episode, next -> viewModel.onIntent(PodcastDetailIntent.Enqueue(episode, next)) },
            onDownload = { viewModel.onIntent(PodcastDetailIntent.Download(it)) },
            onCancelDownload = { viewModel.onIntent(PodcastDetailIntent.CancelDownload(it)) },
            onRemoveDownload = { viewModel.onIntent(PodcastDetailIntent.RequestDeleteDownload(it)) },
            onConfirmRemoveDownload = { viewModel.onIntent(PodcastDetailIntent.ConfirmDeleteDownload(it)) },
            onDismissRemoveDownload = { viewModel.onIntent(PodcastDetailIntent.DismissDeleteDownload) },
            onMarkPlayed = { viewModel.onIntent(PodcastDetailIntent.MarkPlayed(it)) },
            onMarkUnplayed = { viewModel.onIntent(PodcastDetailIntent.MarkUnplayed(it)) },
            onConfirmMarkOlder = { viewModel.onIntent(PodcastDetailIntent.ConfirmMarkOlder(it)) },
            onDismissMarkOlder = { viewModel.onIntent(PodcastDetailIntent.DismissMarkOlder) },
            onToggleSelection = { viewModel.onIntent(PodcastDetailIntent.ToggleSelection(it)) },
            onClearSelection = { viewModel.onIntent(PodcastDetailIntent.ClearSelection) },
            onActOnSelection = { viewModel.onIntent(PodcastDetailIntent.ActOnSelection(it)) },
            onSetSaved = { episode, saved -> viewModel.onIntent(PodcastDetailIntent.SetSaved(episode, saved)) },
            onPlaybackChange = { viewModel.onIntent(PodcastDetailIntent.SetPlayback(it)) },
            onNotificationsChange = { viewModel.onIntent(PodcastDetailIntent.SetNotifications(it)) },
        ),
    )
}

data class PodcastDetailActions(
    val onBack: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onFilterSelected: (EpisodeFilter) -> Unit = {},
    val onOldestFirstChange: (Boolean) -> Unit = {},
    val onEpisodeClick: (Episode) -> Unit = {},
    val onRequestMarkOlder: (OlderMark) -> Unit = {},
    val onPlay: (Episode) -> Unit = {},
    /** "Play next" when the flag is set, else "Add to queue" (18.6). */
    val onEnqueue: (Episode, Boolean) -> Unit = { _, _ -> },
    val onDownload: (Episode) -> Unit = {},
    val onCancelDownload: (Episode) -> Unit = {},
    val onRemoveDownload: (Episode) -> Unit = {},
    val onConfirmRemoveDownload: (Episode) -> Unit = {},
    val onDismissRemoveDownload: () -> Unit = {},
    val onMarkPlayed: (Episode) -> Unit = {},
    val onMarkUnplayed: (Episode) -> Unit = {},
    val onConfirmMarkOlder: (OlderMark) -> Unit = {},
    val onDismissMarkOlder: () -> Unit = {},
    /** Several episodes at once (18.11). */
    val onToggleSelection: (Episode) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onActOnSelection: (EpisodeAction) -> Unit = {},
    val onSetSaved: (Episode, Boolean) -> Unit = { _, _ -> },
    /** The podcast's own speed and skipped start and end (18.14). */
    val onPlaybackChange: (PodcastPlayback) -> Unit = {},
    /** Whether this podcast's new episodes notify (18.18). */
    val onNotificationsChange: (Boolean) -> Unit = {},
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
        topBar = {
            if (state.selection.isNotEmpty()) {
                EpisodeSelectionBar(state.selection.size, actions.onActOnSelection, actions.onClearSelection)
            } else {
                PodcastDetailTopBar(
                    scrollBehavior,
                    state.podcast?.title?.takeIf { headerGone },
                    canShare = state.podcast != null,
                    oldestFirst = state.oldestFirst,
                    podcast = state.podcast,
                    actions,
                )
            }
        },
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
    canShare: Boolean,
    oldestFirst: Boolean,
    podcast: Podcast?,
    actions: PodcastDetailActions,
) {
    TopAppBar(
        title = { title?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        navigationIcon = {
            IconButton(onClick = actions.onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
            }
        },
        actions = {
            podcast?.let { PlaybackButton(it, actions) }
            OrderMenu(oldestFirst, actions.onOldestFirstChange)
            if (canShare) {
                IconButton(onClick = actions.onShare) {
                    Icon(Icons.Rounded.Share, contentDescription = stringResource(Res.string.share))
                }
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
private fun OrderMenu(oldestFirst: Boolean, onChange: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = stringResource(Res.string.library_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            mapOf(false to Res.string.episodes_newest_first, true to Res.string.episodes_oldest_first)
                .forEach { (option, label) ->
                    val chosen = option == oldestFirst
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            expanded = false
                            onChange(option)
                        },
                        trailingIcon = { if (chosen) Icon(Icons.Rounded.Check, contentDescription = null) },
                        modifier = Modifier.semantics { selected = chosen },
                    )
                }
        }
    }
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
                modifier = Modifier.readableWidth().padding(vertical = Spacing.s),
                contentPadding = PaddingValues(horizontal = Spacing.l),
            )
        }
        items(
            count = episodes.itemCount,
            key = episodes.itemKey { it.id },
            contentType = episodes.itemContentType { "episode" }
        ) { index ->
            episodes[index]?.let { episode ->
                val isCurrent = playerState.currentEpisode?.id == episode.id
                seasonStartingAt(index) { episodes.peek(it)?.season }?.let { SeasonHeader(it) }
                EpisodeListItem(
                    episode = episode,
                    modifier = Modifier.readableWidth(),
                    isBuffering = isCurrent && playerState.isBuffering,
                    isPlaying = isCurrent && playerState.isPlaying,
                    downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
                    onClick = { actions.onEpisodeClick(episode) },
                    onPlayClick = { actions.onPlay(episode) },
                    onPlayNext = { actions.onEnqueue(episode, true) },
                    onAddToQueue = { actions.onEnqueue(episode, false) },
                    onMarkPlayed = { actions.onMarkPlayed(episode) },
                    onMarkUnplayed = { actions.onMarkUnplayed(episode) },
                    onMarkOlderPlayed = { actions.onRequestMarkOlder(OlderMark(episode, played = true)) },
                    onMarkOlderUnplayed = { actions.onRequestMarkOlder(OlderMark(episode, played = false)) },
                    onDownloadClick = { actions.onDownload(episode) },
                    onCancelDownloadClick = { actions.onCancelDownload(episode) },
                    onDeleteClick = { actions.onRemoveDownload(episode) },
                    onSelect = { actions.onToggleSelection(episode) },
                    onToggleSaved = { actions.onSetSaved(episode, !episode.isSaved) },
                    selecting = state.selection.isNotEmpty(),
                    selected = state.selection.any { it.id == episode.id },
                )
            }
        }
    }
}

@Composable
private fun SeasonHeader(season: Int) {
    Text(
        text = stringResource(Res.string.season_number, season),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .readableWidth()
            .padding(horizontal = Spacing.l)
            .padding(top = Spacing.m, bottom = Spacing.xs)
            .semantics { heading() },
    )
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
