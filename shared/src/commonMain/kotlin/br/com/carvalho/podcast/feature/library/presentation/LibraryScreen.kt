package br.com.carvalho.podcast.feature.library.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.core.animateDpAsState
import br.com.carvalho.podcast.presentation.component.PodcastCard
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
                        "Biblioteca",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.onRefreshAll() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar Tudo")
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
            val fabPadding by animateDpAsState(if (isPlayerVisible) 64.dp else 0.dp)
            FloatingActionButton(
                onClick = { viewModel.onAddClicked() },
                modifier = Modifier.padding(bottom = fabPadding)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Adicionar Podcast")
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
                    Text("Nenhum podcast encontrado")
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
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
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
        title = { Text("Excluir Podcast") },
        text = {
            Text("Tem certeza que deseja excluir o podcast \"$podcastTitle\" e todos os seus episódios?")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Excluir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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
        title = { Text("Adicionar Podcast") },
        text = {
            Column {
                Text("Insira a URL do feed RSS:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
