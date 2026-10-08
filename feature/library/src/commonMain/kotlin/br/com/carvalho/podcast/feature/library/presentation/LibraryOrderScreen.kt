package br.com.carvalho.podcast.feature.library.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.PodcastListItem
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.library_move_down
import br.com.carvalho.podcast.core.ui.generated.resources.library_move_up
import br.com.carvalho.podcast.core.ui.generated.resources.library_organize_hint
import br.com.carvalho.podcast.core.ui.generated.resources.library_organize_title
import br.com.carvalho.podcast.core.ui.generated.resources.library_reorder
import br.com.carvalho.podcast.domain.model.LibraryEntry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LibraryOrderScreen(onBack: () -> Unit, viewModel: LibraryOrderViewModel = koinViewModel()) {
    val podcasts by viewModel.podcasts.collectAsState()
    LibraryOrderContent(podcasts, onReorder = viewModel::reorder, onBack = onBack)
}

/**
 * The podcasts in their custom order, always as a list: drag by the handle, or use "move up/down" with a screen
 * reader. Each drop is saved, so leaving the screen loses nothing (ADR 0007).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryOrderContent(entries: List<LibraryEntry>, onReorder: (List<String>) -> Unit, onBack: () -> Unit) {
    // Shown while dragging; saved when the drag ends, then replaced by the order the database sends back.
    var shown by remember(entries) { mutableStateOf(entries) }
    val listState = rememberLazyListState()
    // By key, not index: the hint above the podcasts is an item of the same list.
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        shown = shown.movedByKey(from.key, to.key)
    }

    Scaffold(
        topBar = { OrderTopBar(onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                Text(
                    stringResource(Res.string.library_organize_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.readableWidth().padding(start = Spacing.s, end = Spacing.s, bottom = Spacing.s),
                )
            }
            itemsIndexed(items = shown, key = { _, entry -> entry.podcast.id }) { index, entry ->
                val podcast = entry.podcast
                ReorderableItem(reorderState, key = podcast.id) {
                    PodcastListItem(
                        title = podcast.title,
                        author = podcast.author,
                        imageUrl = podcast.imageUrl,
                        supportingText = null,
                        onClick = {},
                        modifier = Modifier.readableWidth().moveActions(shown, index, onReorder),
                        dragHandle = {
                            Icon(
                                Icons.Rounded.DragHandle,
                                contentDescription = stringResource(Res.string.library_reorder),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .draggableHandle(onDragStopped = { onReorder(shown.ids()) })
                                    .size(Sizes.touchTarget)
                                    .padding(Spacing.m),
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(stringResource(Res.string.library_organize_title), modifier = Modifier.semantics { heading() })
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

/** Reordering without dragging (TalkBack, VoiceOver): "move up" and "move down" on each podcast. */
@Composable
private fun Modifier.moveActions(entries: List<LibraryEntry>, index: Int, onReorder: (List<String>) -> Unit): Modifier {
    val up = stringResource(Res.string.library_move_up)
    val down = stringResource(Res.string.library_move_down)
    return semantics {
        customActions = listOfNotNull(
            CustomAccessibilityAction(up) {
                onReorder(entries.moved(index, index - 1).ids())
                true
            }
                .takeIf { index > 0 },
            CustomAccessibilityAction(down) {
                onReorder(entries.moved(index, index + 1).ids())
                true
            }
                .takeIf { index < entries.lastIndex },
        )
    }
}

private fun List<LibraryEntry>.moved(from: Int, to: Int) = toMutableList().apply { add(to, removeAt(from)) }

/** Moves the podcast with key [from] to where [to] is; keys that are not podcasts (the hint) change nothing. */
internal fun List<LibraryEntry>.movedByKey(from: Any, to: Any): List<LibraryEntry> {
    val fromIndex = indexOfFirst { it.podcast.id == from }
    val toIndex = indexOfFirst { it.podcast.id == to }
    return if (fromIndex == -1 || toIndex == -1) this else moved(fromIndex, toIndex)
}

private fun List<LibraryEntry>.ids() = map { it.podcast.id }
