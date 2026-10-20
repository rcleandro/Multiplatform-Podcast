package br.com.carvalho.podcast.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.core.designsystem.component.ErrorState
import br.com.carvalho.podcast.core.designsystem.component.FilterChipRow
import br.com.carvalho.podcast.core.designsystem.component.FilterOption
import br.com.carvalho.podcast.core.designsystem.component.PodcastSnackbarHost
import br.com.carvalho.podcast.core.designsystem.component.SectionTitle
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.clear
import br.com.carvalho.podcast.core.ui.generated.resources.clear_history
import br.com.carvalho.podcast.core.ui.generated.resources.clear_history_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.dismiss_all_new
import br.com.carvalho.podcast.core.ui.generated.resources.downloads_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.downloads_storage_used
import br.com.carvalho.podcast.core.ui.generated.resources.episodes_tab
import br.com.carvalho.podcast.core.ui.generated.resources.error_loading_results
import br.com.carvalho.podcast.core.ui.generated.resources.filter_all
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.filter_history
import br.com.carvalho.podcast.core.ui.generated.resources.filter_in_progress
import br.com.carvalho.podcast.core.ui.generated.resources.filter_new
import br.com.carvalho.podcast.core.ui.generated.resources.filter_saved
import br.com.carvalho.podcast.core.ui.generated.resources.history_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.history_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.in_progress_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.in_progress_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.new_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.new_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.no_downloads
import br.com.carvalho.podcast.core.ui.generated.resources.saved_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.saved_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.search_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.search_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.search_placeholder
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_new
import br.com.carvalho.podcast.core.ui.generated.resources.try_again
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.SmartPlaylist
import br.com.carvalho.podcast.domain.usecase.EpisodeAction
import br.com.carvalho.podcast.presentation.MessageEffect
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.presentation.component.EpisodeSelectionBar
import br.com.carvalho.podcast.presentation.component.MarkOlderDialog
import br.com.carvalho.podcast.presentation.component.OlderMark
import br.com.carvalho.podcast.presentation.component.SearchField
import br.com.carvalho.podcast.presentation.format.DateGroup
import br.com.carvalho.podcast.presentation.format.dateGroup
import br.com.carvalho.podcast.presentation.format.storageSize
import br.com.carvalho.podcast.presentation.format.text
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = koinViewModel(),
    onEpisodeClick: (String, String) -> Unit,
    onPodcastClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedResults = viewModel.pagedResults.collectAsLazyPagingItems()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onIntent(SearchIntent.Refresh) }
    MessageEffect(viewModel.messages, snackbarHostState)

    SearchContent(
        state = uiState,
        results = pagedResults,
        playerState = playerState,
        activeDownloads = activeDownloads,
        actions = SearchActions(
            onQueryChange = { viewModel.onIntent(SearchIntent.ChangeQuery(it)) },
            onFilterChange = { viewModel.onIntent(SearchIntent.ChangeFilter(it)) },
            onEpisodeClick = { onEpisodeClick(it.id, it.podcastId) },
            onPlay = { viewModel.onIntent(SearchIntent.Play(it)) },
            onEnqueue = { episode, next -> viewModel.onIntent(SearchIntent.Enqueue(episode, next)) },
            onDownload = { viewModel.onIntent(SearchIntent.Download(it)) },
            onCancelDownload = { viewModel.onIntent(SearchIntent.CancelDownload(it)) },
            onRemoveDownload = { viewModel.onIntent(SearchIntent.RequestDeleteDownload(it)) },
            onConfirmRemoveDownload = { viewModel.onIntent(SearchIntent.ConfirmDeleteDownload(it)) },
            onDismissRemoveDownload = { viewModel.onIntent(SearchIntent.DismissDeleteDownload) },
            onMarkPlayed = { viewModel.onIntent(SearchIntent.SetPlayed(it, played = true)) },
            onDismissNew = { viewModel.onIntent(SearchIntent.DismissNew(it)) },
            onMarkUnplayed = { viewModel.onIntent(SearchIntent.SetPlayed(it, played = false)) },
            onRequestMarkOlder = { viewModel.onIntent(SearchIntent.RequestMarkOlder(it)) },
            onConfirmMarkOlder = { viewModel.onIntent(SearchIntent.ConfirmMarkOlder(it)) },
            onDismissMarkOlder = { viewModel.onIntent(SearchIntent.DismissMarkOlder) },
            onPodcastClick = { onPodcastClick(it.podcastId) },
            onToggleSelection = { viewModel.onIntent(SearchIntent.ToggleSelection(it)) },
            onClearSelection = { viewModel.onIntent(SearchIntent.ClearSelection) },
            onActOnSelection = { viewModel.onIntent(SearchIntent.ActOnSelection(it)) },
            onSetSaved = { episode, saved -> viewModel.onIntent(SearchIntent.SetSaved(episode, saved)) },
            onRequestClearHistory = { viewModel.onIntent(SearchIntent.RequestClearHistory) },
            onConfirmClearHistory = { viewModel.onIntent(SearchIntent.ConfirmClearHistory) },
            onDismissClearHistory = { viewModel.onIntent(SearchIntent.DismissClearHistory) },
            onSelectPlaylist = { viewModel.onIntent(SearchIntent.SelectPlaylist(it)) },
            onEditPlaylist = { viewModel.onIntent(SearchIntent.EditPlaylist(it)) },
            onSavePlaylist = { viewModel.onIntent(SearchIntent.SavePlaylist(it)) },
            onDeletePlaylist = { viewModel.onIntent(SearchIntent.DeletePlaylist(it)) },
            onDismissPlaylistEditor = { viewModel.onIntent(SearchIntent.DismissPlaylistEditor) },
            onPlayPlaylist = { viewModel.onIntent(SearchIntent.PlayPlaylist) },
        ),
        snackbarHostState = snackbarHostState,
    )
}

data class SearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onFilterChange: (EpisodeListFilter) -> Unit = {},
    val onEpisodeClick: (Episode) -> Unit = {},
    val onPlay: (Episode) -> Unit = {},
    /** "Play next" when the flag is set, else "Add to queue" (18.6). */
    val onEnqueue: (Episode, Boolean) -> Unit = { _, _ -> },
    val onDownload: (Episode) -> Unit = {},
    val onCancelDownload: (Episode) -> Unit = {},
    val onRemoveDownload: (Episode) -> Unit = {},
    val onConfirmRemoveDownload: (Episode) -> Unit = {},
    val onDismissRemoveDownload: () -> Unit = {},
    val onMarkPlayed: (Episode) -> Unit = {},
    /** Takes an episode out of "New", or all of them with null (18.7). */
    val onDismissNew: (Episode?) -> Unit = {},
    val onMarkUnplayed: (Episode) -> Unit = {},
    val onRequestMarkOlder: (OlderMark) -> Unit = {},
    val onConfirmMarkOlder: (OlderMark) -> Unit = {},
    val onDismissMarkOlder: () -> Unit = {},
    val onPodcastClick: (Episode) -> Unit = {},
    /** Several episodes at once (18.11). */
    val onToggleSelection: (Episode) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onActOnSelection: (EpisodeAction) -> Unit = {},
    val onSetSaved: (Episode, Boolean) -> Unit = { _, _ -> },
    /** "Clear history": asked, confirmed or given up (18.12). */
    val onRequestClearHistory: () -> Unit = {},
    val onConfirmClearHistory: () -> Unit = {},
    val onDismissClearHistory: () -> Unit = {},
    /** Smart playlists (19.4): list one, open the editor (null for a new one), save, delete, play all. */
    val onSelectPlaylist: (SmartPlaylist) -> Unit = {},
    val onEditPlaylist: (SmartPlaylist?) -> Unit = {},
    val onSavePlaylist: (SmartPlaylist) -> Unit = {},
    val onDeletePlaylist: (SmartPlaylist) -> Unit = {},
    val onDismissPlaylistEditor: () -> Unit = {},
    val onPlayPlaylist: () -> Unit = {},
)

/** The fixed "Episodes" title over the search field and filters, which collapse with [scrollState]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    state: SearchUiState,
    scrollState: TopAppBarState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (EpisodeListFilter) -> Unit,
    actions: SearchActions,
) {
    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).readableWidth()) {
        Text(
            text = stringResource(Res.string.episodes_tab),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(start = Spacing.l, end = Spacing.l, top = Spacing.l)
                .semantics { heading() },
        )
        Column(modifier = Modifier.collapsingWith(scrollState)) {
            SearchField(
                query = state.searchQuery,
                onQueryChange = onQueryChange,
                placeholder = stringResource(Res.string.search_placeholder),
            )
            // The fixed filters, then the smart playlists (19.4), then "New playlist", which opens the editor.
            val playlists = state.playlists
            FilterChipRow(
                options = filters.map { (_, label) -> FilterOption(stringResource(label)) } +
                    playlists.map { FilterOption(it.name) } +
                    FilterOption("+ " + stringResource(Res.string.smart_playlist_new)),
                selectedIndex = state.playlist?.let { filters.size + playlists.indexOf(it) }
                    ?: filters.indexOfFirst { it.first == state.filter }.coerceAtLeast(0),
                onSelected = {
                    when {
                        it < filters.size -> onFilterChange(filters[it].first)
                        it < filters.size + playlists.size -> actions.onSelectPlaylist(playlists[it - filters.size])
                        else -> actions.onEditPlaylist(null)
                    }
                },
                modifier = Modifier.padding(bottom = Spacing.s),
                contentPadding = PaddingValues(horizontal = Spacing.l),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    state: SearchUiState,
    results: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: SearchActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val listState = rememberLazyListState()
    val onFilterChange = rememberScrollToTopOnFilterChange(state.filter, results, listState, actions.onFilterChange)
    // Search and filters leave the room to the list while it scrolls down and come back as soon as it scrolls up.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (state.selection.isNotEmpty()) {
                EpisodeSelectionBar(state.selection.size, actions.onActOnSelection, actions.onClearSelection)
            } else {
                SearchTopBar(state, scrollBehavior.state, actions.onQueryChange, onFilterChange, actions)
            }
        },
        snackbarHost = { PodcastSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets()
    ) { padding ->
        val refresh = results.loadState.refresh
        when {
            results.itemCount == 0 && refresh is LoadState.Error -> ErrorState(
                icon = Icons.Rounded.CloudOff,
                title = stringResource(Res.string.error_loading_results),
                message = null,
                actionLabel = stringResource(Res.string.try_again),
                onAction = results::retry,
                modifier = Modifier.padding(padding),
            )
            results.itemCount == 0 && refresh is LoadState.NotLoading ->
                EmptyFilter(state, actions.onEditPlaylist, Modifier.padding(padding))
            else -> SearchResults(
                state,
                results,
                playerState,
                activeDownloads,
                actions,
                listState,
                Modifier.padding(padding)
            )
        }

        SearchDialogs(state, actions)
    }
}

/** "Dismiss all" above New and "Clear history" above History, when there is something to act on. */
private fun LazyListScope.listAction(state: SearchUiState, count: Int, actions: SearchActions) {
    if (count == 0) return
    val label = when (state.filter) {
        EpisodeListFilter.NEW -> Res.string.dismiss_all_new
        EpisodeListFilter.HISTORY -> Res.string.clear_history
        else -> return
    }
    item(key = "list-action-${state.filter}") {
        Box(Modifier.readableWidth().padding(horizontal = Spacing.s), contentAlignment = Alignment.CenterEnd) {
            TextButton(
                onClick = {
                    if (state.filter == EpisodeListFilter.NEW) {
                        actions.onDismissNew(null)
                    } else {
                        actions.onRequestClearHistory()
                    }
                },
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun SearchDialogs(state: SearchUiState, actions: SearchActions) {
    state.editingPlaylist?.let {
        SmartPlaylistEditor(it, actions.onSavePlaylist, actions.onDeletePlaylist, actions.onDismissPlaylistEditor)
    }
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
    state.olderMark?.let { MarkOlderDialog(it, actions.onConfirmMarkOlder, actions.onDismissMarkOlder) }
    if (state.clearHistoryConfirmation) {
        ConfirmDialog(
            title = stringResource(Res.string.clear_history),
            message = stringResource(Res.string.clear_history_confirmation),
            confirmLabel = stringResource(Res.string.clear),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = actions.onConfirmClearHistory,
            onDismiss = actions.onDismissClearHistory,
        )
    }
}

/**
 * Shrinks the content by [state]'s offset, as a top app bar does, so the nested scroll of an enter-always behavior
 * slides it away and back. Its full height is the limit of the offset.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.collapsingWith(state: TopAppBarState): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val limit = -placeable.height.toFloat()
        if (state.heightOffsetLimit != limit) state.heightOffsetLimit = limit
        val offset = state.heightOffset.roundToInt()
        layout(placeable.width, (placeable.height + offset).coerceAtLeast(0)) { placeable.place(0, offset) }
    }

// "Downloaded" only where downloads survive (not on the Web).
private val filters = listOfNotNull(
    EpisodeListFilter.NEW to Res.string.filter_new,
    EpisodeListFilter.ALL to Res.string.filter_all,
    EpisodeListFilter.IN_PROGRESS to Res.string.filter_in_progress,
    EpisodeListFilter.SAVED to Res.string.filter_saved,
    (EpisodeListFilter.DOWNLOADED to Res.string.filter_downloaded).takeIf { supportsDownloads },
    EpisodeListFilter.HISTORY to Res.string.filter_history,
)

/** Each filter says why it is empty; a search that finds nothing says so whatever the filter. */
@Composable
private fun EmptyFilter(state: SearchUiState, onEditPlaylist: (SmartPlaylist) -> Unit, modifier: Modifier) {
    val playlist = state.playlist
    when {
        playlist != null -> EmptyPlaylist(playlist, onEditPlaylist, modifier)
        state.searchQuery.isNotBlank() || state.filter == EpisodeListFilter.ALL -> EmptyState(
            icon = Icons.Rounded.Search,
            title = stringResource(Res.string.search_empty_title),
            message = stringResource(Res.string.search_empty_message),
            modifier = modifier,
        )
        state.filter == EpisodeListFilter.NEW -> EmptyState(
            icon = Icons.Rounded.Inbox,
            title = stringResource(Res.string.new_empty_title),
            message = stringResource(Res.string.new_empty_message),
            modifier = modifier,
        )
        state.filter == EpisodeListFilter.HISTORY -> EmptyState(
            icon = Icons.Rounded.History,
            title = stringResource(Res.string.history_empty_title),
            message = stringResource(Res.string.history_empty_message),
            modifier = modifier,
        )
        state.filter == EpisodeListFilter.SAVED -> EmptyState(
            icon = Icons.Rounded.BookmarkBorder,
            title = stringResource(Res.string.saved_empty_title),
            message = stringResource(Res.string.saved_empty_message),
            modifier = modifier,
        )
        state.filter == EpisodeListFilter.IN_PROGRESS -> EmptyState(
            icon = Icons.Rounded.Timelapse,
            title = stringResource(Res.string.in_progress_empty_title),
            message = stringResource(Res.string.in_progress_empty_message),
            modifier = modifier,
        )
        else -> EmptyState(
            icon = Icons.Rounded.DownloadDone,
            title = stringResource(Res.string.no_downloads),
            message = stringResource(Res.string.downloads_empty_message),
            modifier = modifier,
        )
    }
}

@Composable
private fun SearchResults(
    state: SearchUiState,
    results: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: SearchActions,
    listState: LazyListState,
    modifier: Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Sizes.listBottomInset)
    ) {
        val playlist = state.playlist
        if (playlist != null) {
            if (results.itemCount > 0) playlistActions(playlist, actions)
        } else {
            listAction(state, results.itemCount, actions)
        }
        if (state.filter == EpisodeListFilter.DOWNLOADED) {
            item {
                Text(
                    text = stringResource(Res.string.downloads_storage_used, storageSize(state.usedBytes).text()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.readableWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
                )
            }
        }
        // A header wherever the date group changes, held at the top while its episodes scroll by. Peeking does not
        // load pages; episodes not loaded yet stay in the group before them.
        var group: DateGroup? = null
        val now = getCurrentTimestamp()
        for (index in 0 until results.itemCount) {
            // History groups by when each episode last played; the others by when it came out.
            val episodeGroup = results.peek(index)?.let {
                val played = it.lastPlayedAt.takeIf { state.filter == EpisodeListFilter.HISTORY }
                val date = played ?: it.publishDate
                dateGroup(date, now)
            }
            if (episodeGroup != null && episodeGroup != group) {
                group = episodeGroup
                stickyHeader(key = "group-$episodeGroup", contentType = "group") {
                    SectionTitle(
                        text = episodeGroup.text(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .readableWidth()
                            .padding(horizontal = Spacing.l, vertical = Spacing.s),
                    )
                }
            }
            item(key = results.peek(index)?.id ?: "placeholder-$index", contentType = "episode") {
                EpisodeRowAt(index, results, playerState, activeDownloads, actions, state.selection)
            }
        }
        if (results.loadState.append is LoadState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.l), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * Another filter starts at its top. Its items arrive after the query's debounce, and until then the old filter's
 * items stay on screen; scrolling right away would let the list follow the old top item into the new order. So the
 * wait for the next refresh (loading, then done) starts before the filter is changed, and the scroll comes after it.
 */
@Composable
private fun rememberScrollToTopOnFilterChange(
    current: EpisodeListFilter,
    results: LazyPagingItems<Episode>,
    listState: LazyListState,
    onFilterChange: (EpisodeListFilter) -> Unit,
): (EpisodeListFilter) -> Unit {
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Job?>(null) }
    return { filter ->
        if (filter != current) {
            pending?.cancel()
            pending = scope.launch {
                snapshotFlow { results.loadState.refresh }
                    .dropWhile { it !is LoadState.Loading }
                    .first { it is LoadState.NotLoading }
                listState.scrollToItem(0)
            }
        }
        onFilterChange(filter)
    }
}

/** Pill-shaped search field; the indicator line takes the container color so it does not show. */
@Composable
private fun EpisodeRowAt(
    index: Int,
    results: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: SearchActions,
    selection: List<Episode>,
) {
    results[index]?.let { episode ->
        val isCurrent = playerState.currentEpisode?.id == episode.id
        EpisodeListItem(
            episode = episode,
            podcastTitle = episode.podcastTitle,
            modifier = Modifier.readableWidth(),
            isBuffering = isCurrent && playerState.isBuffering,
            isPlaying = isCurrent && playerState.isPlaying,
            downloadStatus = activeDownloads[episode.id] ?: DownloadStatus.Idle,
            onClick = { actions.onEpisodeClick(episode) },
            onPlayClick = { actions.onPlay(episode) },
            onPlayNext = { actions.onEnqueue(episode, true) },
            onAddToQueue = { actions.onEnqueue(episode, false) },
            onDownloadClick = { actions.onDownload(episode) },
            onCancelDownloadClick = { actions.onCancelDownload(episode) },
            onDeleteClick = { actions.onRemoveDownload(episode) },
            onMarkPlayed = { actions.onMarkPlayed(episode) },
            onDismissNew = { actions.onDismissNew(episode) }.takeIf { episode.isNew },
            onMarkUnplayed = { actions.onMarkUnplayed(episode) },
            onMarkOlderPlayed = { actions.onRequestMarkOlder(OlderMark(episode, played = true)) },
            onMarkOlderUnplayed = { actions.onRequestMarkOlder(OlderMark(episode, played = false)) },
            onGoToPodcast = { actions.onPodcastClick(episode) },
            onSelect = { actions.onToggleSelection(episode) },
            onToggleSaved = { actions.onSetSaved(episode, !episode.isSaved) },
            selecting = selection.isNotEmpty(),
            selected = selection.any { it.id == episode.id },
        )
    }
}
