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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
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
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.downloads_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.downloads_storage_used
import br.com.carvalho.podcast.core.ui.generated.resources.episodes_tab
import br.com.carvalho.podcast.core.ui.generated.resources.error_loading_results
import br.com.carvalho.podcast.core.ui.generated.resources.filter_all
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.filter_in_progress
import br.com.carvalho.podcast.core.ui.generated.resources.in_progress_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.in_progress_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.no_downloads
import br.com.carvalho.podcast.core.ui.generated.resources.search
import br.com.carvalho.podcast.core.ui.generated.resources.search_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.search_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.search_placeholder
import br.com.carvalho.podcast.core.ui.generated.resources.try_again
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.presentation.MessageEffect
import br.com.carvalho.podcast.presentation.component.EpisodeListItem
import br.com.carvalho.podcast.presentation.component.MarkOlderDialog
import br.com.carvalho.podcast.presentation.component.OlderMark
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
            onDownload = { viewModel.onIntent(SearchIntent.Download(it)) },
            onCancelDownload = { viewModel.onIntent(SearchIntent.CancelDownload(it)) },
            onRemoveDownload = { viewModel.onIntent(SearchIntent.RequestDeleteDownload(it)) },
            onConfirmRemoveDownload = { viewModel.onIntent(SearchIntent.ConfirmDeleteDownload(it)) },
            onDismissRemoveDownload = { viewModel.onIntent(SearchIntent.DismissDeleteDownload) },
            onMarkPlayed = { viewModel.onIntent(SearchIntent.SetPlayed(it, played = true)) },
            onMarkUnplayed = { viewModel.onIntent(SearchIntent.SetPlayed(it, played = false)) },
            onRequestMarkOlder = { viewModel.onIntent(SearchIntent.RequestMarkOlder(it)) },
            onConfirmMarkOlder = { viewModel.onIntent(SearchIntent.ConfirmMarkOlder(it)) },
            onDismissMarkOlder = { viewModel.onIntent(SearchIntent.DismissMarkOlder) },
            onPodcastClick = { onPodcastClick(it.podcastId) },
        ),
        snackbarHostState = snackbarHostState,
    )
}

data class SearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onFilterChange: (EpisodeListFilter) -> Unit = {},
    val onEpisodeClick: (Episode) -> Unit = {},
    val onPlay: (Episode) -> Unit = {},
    val onDownload: (Episode) -> Unit = {},
    val onCancelDownload: (Episode) -> Unit = {},
    val onRemoveDownload: (Episode) -> Unit = {},
    val onConfirmRemoveDownload: (Episode) -> Unit = {},
    val onDismissRemoveDownload: () -> Unit = {},
    val onMarkPlayed: (Episode) -> Unit = {},
    val onMarkUnplayed: (Episode) -> Unit = {},
    val onRequestMarkOlder: (OlderMark) -> Unit = {},
    val onConfirmMarkOlder: (OlderMark) -> Unit = {},
    val onDismissMarkOlder: () -> Unit = {},
    val onPodcastClick: (Episode) -> Unit = {},
)

/** The fixed "Episodes" title over the search field and filters, which collapse with [scrollState]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    state: SearchUiState,
    scrollState: TopAppBarState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (EpisodeListFilter) -> Unit,
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
            SearchField(query = state.searchQuery, onQueryChange = onQueryChange)
            FilterChipRow(
                options = filters.map { (_, label) -> FilterOption(stringResource(label)) },
                selectedIndex = filters.indexOfFirst { it.first == state.filter }.coerceAtLeast(0),
                onSelected = { onFilterChange(filters[it].first) },
                modifier = Modifier.padding(start = Spacing.l, end = Spacing.l, bottom = Spacing.s),
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
        topBar = { SearchTopBar(state, scrollBehavior.state, actions.onQueryChange, onFilterChange) },
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
                EmptyFilter(state, Modifier.padding(padding))
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
    EpisodeListFilter.ALL to Res.string.filter_all,
    EpisodeListFilter.IN_PROGRESS to Res.string.filter_in_progress,
    (EpisodeListFilter.DOWNLOADED to Res.string.filter_downloaded).takeIf { supportsDownloads },
)

/** Each filter says why it is empty; a search that finds nothing says so whatever the filter. */
@Composable
private fun EmptyFilter(state: SearchUiState, modifier: Modifier) {
    when {
        state.searchQuery.isNotBlank() || state.filter == EpisodeListFilter.ALL -> EmptyState(
            icon = Icons.Rounded.Search,
            title = stringResource(Res.string.search_empty_title),
            message = stringResource(Res.string.search_empty_message),
            modifier = modifier,
        )
        state.filter == EpisodeListFilter.IN_PROGRESS -> EmptyState(
            icon = Icons.Rounded.PlayArrow,
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
            val episodeGroup = results.peek(index)?.let { dateGroup(it.publishDate, now) }
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
                EpisodeRowAt(index, results, playerState, activeDownloads, actions)
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
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val container = MaterialTheme.colorScheme.surfaceContainer
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(stringResource(Res.string.search_placeholder), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = stringResource(Res.string.search)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(Res.string.clear))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = container,
            unfocusedIndicatorColor = container,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
    )
}

@Composable
private fun EpisodeRowAt(
    index: Int,
    results: LazyPagingItems<Episode>,
    playerState: PlayerState,
    activeDownloads: Map<String, DownloadStatus>,
    actions: SearchActions,
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
            onDownloadClick = { actions.onDownload(episode) },
            onCancelDownloadClick = { actions.onCancelDownload(episode) },
            onDeleteClick = { actions.onRemoveDownload(episode) },
            onMarkPlayed = { actions.onMarkPlayed(episode) },
            onMarkUnplayed = { actions.onMarkUnplayed(episode) },
            onMarkOlderPlayed = { actions.onRequestMarkOlder(OlderMark(episode, played = true)) },
            onMarkOlderUnplayed = { actions.onRequestMarkOlder(OlderMark(episode, played = false)) },
            onGoToPodcast = { actions.onPodcastClick(episode) },
        )
    }
}
