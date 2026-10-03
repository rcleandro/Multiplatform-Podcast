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
import androidx.compose.runtime.LaunchedEffect
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
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.add
import br.com.carvalho.podcast.shared.add_podcast
import br.com.carvalho.podcast.shared.cancel
import br.com.carvalho.podcast.shared.delete
import br.com.carvalho.podcast.shared.delete_podcast
import br.com.carvalho.podcast.shared.delete_podcast_confirmation
import br.com.carvalho.podcast.shared.library_empty_message
import br.com.carvalho.podcast.shared.library_title
import br.com.carvalho.podcast.shared.no_podcasts_found
import br.com.carvalho.podcast.shared.podcast_options
import br.com.carvalho.podcast.shared.refresh_all
import br.com.carvalho.podcast.shared.rss_url_label
import br.com.carvalho.podcast.shared.rss_url_placeholder
import org.jetbrains.compose.resources.getString
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

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(getString(it))
            viewModel.clearError()
        }
    }

    LibraryContent(
        state = uiState,
        isPlayerVisible = isPlayerVisible,
        snackbarHostState = snackbarHostState,
        actions = LibraryActions(
            onPodcastClick = onPodcastClick,
            onPodcastLongClick = viewModel::onDeleteClicked,
            onRefresh = viewModel::onRefreshAll,
            onAddClick = viewModel::onAddClicked,
            onUrlChange = viewModel::onUrlChanged,
            onAddConfirm = viewModel::addPodcast,
            onAddDismiss = viewModel::onDismissAddDialog,
            onDeleteConfirm = viewModel::confirmDelete,
            onDeleteDismiss = viewModel::onDismissDeleteDialog,
        ),
    )
}

data class LibraryActions(
    val onPodcastClick: (String) -> Unit = {},
    val onPodcastLongClick: (Podcast) -> Unit = {},
    val onRefresh: () -> Unit = {},
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
    val fabPadding by animateDpAsState(
        targetValue = if (isPlayerVisible) Sizes.miniPlayerHeight else 0.dp,
        animationSpec = tween(Motion.MEDIUM, easing = Motion.Standard)
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { LibraryTopBar(scrollBehavior, actions.onRefresh) },
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
            LibraryBody(state, actions)
        }
        LibraryDialogs(state, actions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(scrollBehavior: TopAppBarScrollBehavior, onRefresh: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.library_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        },
        actions = {
            IconButton(onClick = onRefresh) {
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

@Composable
private fun LibraryBody(state: LibraryUiState, actions: LibraryActions) {
    when {
        state.isLoading -> LoadingState()
        state.podcasts.isEmpty() -> EmptyState(
            icon = Icons.Rounded.Mic,
            title = stringResource(Res.string.no_podcasts_found),
            message = stringResource(Res.string.library_empty_message),
            actionLabel = stringResource(Res.string.add_podcast),
            onAction = actions.onAddClick,
        )
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = Sizes.artworkM),
            contentPadding = PaddingValues(
                start = Spacing.l,
                top = Spacing.l,
                end = Spacing.l,
                bottom = Sizes.listBottomInset,
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = state.podcasts, key = { it.id }) { podcast ->
                PodcastCard(
                    title = podcast.title,
                    author = podcast.author,
                    imageUrl = podcast.imageUrl,
                    onClick = { actions.onPodcastClick(podcast.id) },
                    onLongClick = { actions.onPodcastLongClick(podcast) },
                    onLongClickLabel = stringResource(Res.string.podcast_options)
                )
            }
        }
    }
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
