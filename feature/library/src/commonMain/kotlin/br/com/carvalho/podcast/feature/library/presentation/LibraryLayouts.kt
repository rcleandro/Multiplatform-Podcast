package br.com.carvalho.podcast.feature.library.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.PodcastCard
import br.com.carvalho.podcast.core.designsystem.component.PodcastListItem
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.library_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_options
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.presentation.format.relativeTime
import br.com.carvalho.podcast.presentation.format.text
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val libraryPadding = PaddingValues(
    start = Spacing.l,
    top = Spacing.l,
    end = Spacing.l,
    // Room for the mini player and the floating "+", so neither covers the last podcast.
    bottom = Sizes.listBottomInset + Sizes.fabClearance,
)

@Composable
internal fun LibraryGrid(entries: List<LibraryEntry>, actions: LibraryActions, state: LazyGridState) {
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
                onLongClickLabel = stringResource(Res.string.podcast_options),
            )
        }
    }
}

@Composable
internal fun LibraryList(entries: List<LibraryEntry>, actions: LibraryActions, state: LazyListState) {
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
