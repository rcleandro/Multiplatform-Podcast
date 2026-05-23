package br.com.carvalho.podcast.feature.library.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.AppDimensions
import br.com.carvalho.podcast.presentation.component.PodcastCard
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.add
import br.com.carvalho.podcast.shared.add_podcast
import br.com.carvalho.podcast.shared.cancel
import br.com.carvalho.podcast.shared.delete
import br.com.carvalho.podcast.shared.delete_podcast
import br.com.carvalho.podcast.shared.delete_podcast_confirmation
import br.com.carvalho.podcast.shared.library_title
import br.com.carvalho.podcast.shared.no_podcasts_found
import br.com.carvalho.podcast.shared.refresh_all
import br.com.carvalho.podcast.shared.rss_url_label
import br.com.carvalho.podcast.shared.rss_url_placeholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = koinViewModel(),
    isPlayerVisible: Boolean = false,
    onPodcastClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.library_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.onRefreshAll() }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(Res.string.refresh_all)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets(),
        floatingActionButton = {
            val fabPadding by animateDpAsState(if (isPlayerVisible) AppDimensions.miniPlayerHeight else 0.dp)
            FloatingActionButton(
                onClick = { viewModel.onAddClicked() },
                modifier = Modifier.padding(bottom = fabPadding)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(Res.string.add_podcast))
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.onRefreshAll() },
            modifier = Modifier.padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.podcasts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(Res.string.no_podcasts_found))
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = when {
                        maxWidth < 600.dp -> 2
                        maxWidth < 840.dp -> 3
                        maxWidth < 1200.dp -> 4
                        maxWidth < 1600.dp -> 5
                        else -> 6
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(
                            start = AppDimensions.paddingNormal,
                            top = AppDimensions.paddingNormal,
                            end = AppDimensions.paddingNormal,
                            bottom = AppDimensions.miniPlayerHeightWithPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingLarge),
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingLarge),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.podcasts) { podcast ->
                            PodcastCard(
                                podcast = podcast,
                                onClick = { onPodcastClick(podcast.id) },
                                onLongClick = { viewModel.onDeleteClicked(podcast) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isAddDialogOpen) {
            AddPodcastDialog(
                url = uiState.addUrl,
                onUrlChange = { viewModel.onUrlChanged(it) },
                onDismiss = { viewModel.onDismissAddDialog() },
                onConfirm = { viewModel.addPodcast() }
            )
        }

        uiState.podcastToDelete?.let { podcast ->
            DeletePodcastDialog(
                podcastTitle = podcast.title,
                onDismiss = { viewModel.onDismissDeleteDialog() },
                onConfirm = { viewModel.confirmDelete() }
            )
        }
    }
}

@Composable
fun DeletePodcastDialog(
    podcastTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.delete_podcast)) },
        text = {
            Text(stringResource(Res.string.delete_podcast_confirmation, podcastTitle))
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(Res.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
fun AddPodcastDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_podcast)) },
        text = {
            Column {
                Text(stringResource(Res.string.rss_url_label))
                Spacer(modifier = Modifier.height(AppDimensions.spacingMedium))
                TextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.rss_url_placeholder)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(Res.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
