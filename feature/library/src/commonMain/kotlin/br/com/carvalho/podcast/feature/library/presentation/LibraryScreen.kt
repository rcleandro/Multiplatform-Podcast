package br.com.carvalho.podcast.feature.library.presentation

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.selected
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort_first_added
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort_latest_episode
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort_most_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort_recently_added
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort_title
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.Motion
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.core.designsystem.component.LoadingState
import br.com.carvalho.podcast.core.designsystem.component.PodcastCard
import br.com.carvalho.podcast.core.designsystem.component.PodcastListItem
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.presentation.format.relativeTime
import br.com.carvalho.podcast.presentation.format.text
import br.com.carvalho.podcast.core.ui.generated.resources.library_show_grid
import br.com.carvalho.podcast.core.ui.generated.resources.library_show_list
import br.com.carvalho.podcast.core.ui.generated.resources.library_unplayed
import org.jetbrains.compose.resources.pluralStringResource
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.presentation.MessageEffect
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add
import br.com.carvalho.podcast.core.ui.generated.resources.add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.delete_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.delete_podcast_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.library_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.library_title
import br.com.carvalho.podcast.core.ui.generated.resources.no_podcasts_found
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_options
import br.com.carvalho.podcast.core.ui.generated.resources.refresh_all
import br.com.carvalho.podcast.core.ui.generated.resources.rss_url_label
import br.com.carvalho.podcast.core.ui.generated.resources.rss_url_placeholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = koinViewModel(),
    isPlayerVisible: Boolean = false,
    onPodcastClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    MessageEffect(viewModel.messages, snackbarHostState)

    LibraryContent(
        state = uiState,
        isPlayerVisible = isPlayerVisible,
        snackbarHostState = snackbarHostState,
        actions = LibraryActions(
            onPodcastClick = onPodcastClick,
            onPodcastLongClick = { viewModel.onIntent(LibraryIntent.RequestDelete(it)) },
            onRefresh = { viewModel.onIntent(LibraryIntent.RefreshAll) },
            onToggleLayout = { viewModel.onIntent(LibraryIntent.ToggleLayout) },
            onSortChange = { viewModel.onIntent(LibraryIntent.ChangeSort(it)) },
            onAddClick = { viewModel.onIntent(LibraryIntent.OpenAddDialog) },
            onUrlChange = { viewModel.onIntent(LibraryIntent.ChangeUrl(it)) },
            onAddConfirm = { viewModel.onIntent(LibraryIntent.ConfirmAdd) },
            onAddDismiss = { viewModel.onIntent(LibraryIntent.DismissAddDialog) },
            onDeleteConfirm = { viewModel.onIntent(LibraryIntent.ConfirmDelete) },
            onDeleteDismiss = { viewModel.onIntent(LibraryIntent.DismissDelete) },
        ),
    )
}

data class LibraryActions(
    val onPodcastClick: (String) -> Unit = {},
    val onPodcastLongClick: (Podcast) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onToggleLayout: () -> Unit = {},
    val onSortChange: (LibrarySort) -> Unit = {},
    val onAddClick: () -> Unit = {},
    val onUrlChange: (String) -> Unit = {},
    val onAddConfirm: () -> Unit = {},
    val onAddDismiss: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    state: LibraryUiState,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
    isPlayerVisible: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    // A new order starts from its first podcast. It scrolls once the reordered list is on screen: before that, the
    // lazy list would follow the item at the top to its new place.
    var shownSort by remember { mutableStateOf(state.sort) }
    LaunchedEffect(state.sort) {
        if (state.sort != shownSort) {
            shownSort = state.sort
            listState.scrollToItem(0)
            gridState.scrollToItem(0)
        }
    }
    val fabPadding by animateDpAsState(
        targetValue = if (isPlayerVisible) Sizes.miniPlayerHeight else 0.dp,
        animationSpec = tween(Motion.MEDIUM, easing = Motion.Standard)
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { LibraryTopBar(scrollBehavior, state, actions) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = actions.onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = fabPadding)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(Res.string.add_podcast))
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.padding(padding)
        ) {
            LibraryBody(state, actions, listState, gridState)
        }
        LibraryDialogs(state, actions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(scrollBehavior: TopAppBarScrollBehavior, state: LibraryUiState, actions: LibraryActions) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.library_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        },
        actions = {
            SortMenu(state.sort, actions.onSortChange)
            // The button shows the layout it switches to.
            IconButton(onClick = actions.onToggleLayout) {
                if (state.layout == LibraryLayout.GRID) {
                    Icon(Icons.AutoMirrored.Rounded.ViewList, stringResource(Res.string.library_show_list))
                } else {
                    Icon(Icons.Rounded.GridView, stringResource(Res.string.library_show_grid))
                }
            }
            IconButton(onClick = actions.onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(Res.string.refresh_all))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior
    )
}

private val sortLabels = mapOf(
    LibrarySort.TITLE to Res.string.library_sort_title,
    LibrarySort.RECENTLY_ADDED to Res.string.library_sort_recently_added,
    LibrarySort.FIRST_ADDED to Res.string.library_sort_first_added,
    LibrarySort.LATEST_EPISODE to Res.string.library_sort_latest_episode,
    LibrarySort.MOST_UNPLAYED to Res.string.library_sort_most_unplayed,
)

@Composable
private fun SortMenu(current: LibrarySort, onSortChange: (LibrarySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = stringResource(Res.string.library_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sortLabels.forEach { (sort, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        expanded = false
                        onSortChange(sort)
                    },
                    trailingIcon = { if (sort == current) Icon(Icons.Rounded.Check, contentDescription = null) },
                    modifier = Modifier.semantics { selected = sort == current },
                )
            }
        }
    }
}

@Composable
private fun LibraryBody(
    state: LibraryUiState,
    actions: LibraryActions,
    listState: LazyListState,
    gridState: LazyGridState,
) {
    when {
        state.isLoading -> LoadingState()
        state.podcasts.isEmpty() -> EmptyState(
            icon = Icons.Rounded.Mic,
            title = stringResource(Res.string.no_podcasts_found),
            message = stringResource(Res.string.library_empty_message),
            actionLabel = stringResource(Res.string.add_podcast),
            onAction = actions.onAddClick,
        )
        state.layout == LibraryLayout.LIST -> LibraryList(state.podcasts, actions, listState)
        else -> LibraryGrid(state.podcasts, actions, gridState)
    }
}

private val libraryPadding = PaddingValues(
    start = Spacing.l,
    top = Spacing.l,
    end = Spacing.l,
    bottom = Sizes.listBottomInset,
)

@Composable
private fun LibraryGrid(entries: List<LibraryEntry>, actions: LibraryActions, state: LazyGridState) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(minSize = Sizes.artworkM),
        contentPadding = libraryPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = entries, key = { it.podcast.id }) { entry ->
            val podcast = entry.podcast
            PodcastCard(
                title = podcast.title,
                author = podcast.author,
                imageUrl = podcast.imageUrl,
                unplayedCount = entry.unplayedCount,
                onClick = { actions.onPodcastClick(podcast.id) },
                onLongClick = { actions.onPodcastLongClick(podcast) },
                onLongClickLabel = stringResource(Res.string.podcast_options)
            )
        }
    }
}

@Composable
private fun LibraryList(entries: List<LibraryEntry>, actions: LibraryActions, state: LazyListState) {
    LazyColumn(
        state = state,
        contentPadding = libraryPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = entries, key = { it.podcast.id }) { entry ->
            val podcast = entry.podcast
            PodcastListItem(
                title = podcast.title,
                author = podcast.author,
                imageUrl = podcast.imageUrl,
                supportingText = entry.summary(),
                onClick = { actions.onPodcastClick(podcast.id) },
                onLongClick = { actions.onPodcastLongClick(podcast) },
                onLongClickLabel = stringResource(Res.string.podcast_options),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "2 days ago · 3 unplayed", like the episode rows; each half is left out when there is nothing to say. */
@Composable
private fun LibraryEntry.summary(): String? {
    val latest = latestEpisodeDate?.let { relativeTime(it, getCurrentTimestamp())?.text() }
    val unplayed = unplayedCount.takeIf { it > 0 }?.let {
        pluralStringResource(Res.plurals.library_unplayed, it, it)
    }
    return listOfNotNull(latest, unplayed).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun LibraryDialogs(state: LibraryUiState, actions: LibraryActions) {
    if (state.isAddDialogOpen) {
        AddPodcastDialog(
            url = state.addUrl,
            onUrlChange = actions.onUrlChange,
            onDismiss = actions.onAddDismiss,
            onConfirm = actions.onAddConfirm
        )
    }
    state.podcastToDelete?.let { podcast ->
        ConfirmDialog(
            title = stringResource(Res.string.delete_podcast),
            message = stringResource(Res.string.delete_podcast_confirmation, podcast.title),
            confirmLabel = stringResource(Res.string.delete),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = actions.onDeleteConfirm,
            onDismiss = actions.onDeleteDismiss,
        )
    }
}

@Composable
private fun AddPodcastDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_podcast)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text(stringResource(Res.string.rss_url_label)) },
                    placeholder = { Text(stringResource(Res.string.rss_url_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = url.isNotBlank()) { Text(stringResource(Res.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
