package br.com.carvalho.podcast.feature.library.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ContinueCard
import br.com.carvalho.podcast.core.designsystem.component.FilterChipRow
import br.com.carvalho.podcast.core.designsystem.component.FilterOption
import br.com.carvalho.podcast.core.designsystem.component.ItemAction
import br.com.carvalho.podcast.core.designsystem.component.PodcastCard
import br.com.carvalho.podcast.core.designsystem.component.PodcastListItem
import br.com.carvalho.podcast.core.designsystem.component.SectionTitle
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.category_all
import br.com.carvalho.podcast.core.ui.generated.resources.continue_listening
import br.com.carvalho.podcast.core.ui.generated.resources.delete_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.library_podcasts
import br.com.carvalho.podcast.core.ui.generated.resources.library_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_options
import br.com.carvalho.podcast.core.ui.generated.resources.remaining_time
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.presentation.component.remainingDuration
import br.com.carvalho.podcast.presentation.format.label
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
internal fun LibraryGrid(state: LibraryUiState, actions: LibraryActions, gridState: LazyGridState) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = Sizes.artworkM),
        contentPadding = libraryPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = HEADER_KEY, span = { GridItemSpan(maxLineSpan) }) { LibraryHeader(state, actions) }
        items(items = state.podcasts, key = { it.podcast.id }) { entry ->
            val podcast = entry.podcast
            PodcastCard(
                title = podcast.title,
                author = podcast.author,
                imageUrl = podcast.imageUrl,
                unplayedCount = entry.unplayedCount,
                explicit = podcast.explicit,
                onClick = { actions.onPodcastClick(podcast.id) },
                actions = podcastActions(podcast, actions),
                actionsLabel = stringResource(Res.string.podcast_options),
            )
        }
    }
}

@Composable
internal fun LibraryList(state: LibraryUiState, actions: LibraryActions, listState: LazyListState) {
    LazyColumn(
        state = listState,
        // No right margin: each row's "⋮" sits at the edge, like the episode rows.
        contentPadding = PaddingValues(
            start = Spacing.l,
            top = Spacing.l,
            bottom = Sizes.listBottomInset + Sizes.fabClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = HEADER_KEY) { LibraryHeader(state, actions, chipsModifier = Modifier.padding(end = Spacing.l)) }
        items(items = state.podcasts, key = { it.podcast.id }) { entry ->
            val podcast = entry.podcast
            PodcastListItem(
                title = podcast.title,
                author = podcast.author,
                imageUrl = podcast.imageUrl,
                supportingText = entry.summary(),
                explicit = podcast.explicit,
                onClick = { actions.onPodcastClick(podcast.id) },
                actions = podcastActions(podcast, actions),
                actionsLabel = stringResource(Res.string.podcast_options),
                modifier = Modifier.readableWidth(),
            )
        }
    }
}

private const val HEADER_KEY = "header"

/**
 * "Continue listening" and the category chips, in one item that is always first. They load after the podcasts; as
 * separate items added above the first one, the lazy list kept that one in place and pushed them off the top.
 */
@Composable
private fun LibraryHeader(state: LibraryUiState, actions: LibraryActions, chipsModifier: Modifier = Modifier) {
    Column {
        if (state.inProgress.isNotEmpty()) ContinueListening(state.inProgress, actions.onPlay)
        if (state.showsCategories) CategoryChips(state, actions, chipsModifier.padding(bottom = Spacing.s))
    }
}

/** Chips only when they can tell podcasts apart: two categories or more. */
private val LibraryUiState.showsCategories: Boolean get() = categories.size > 1

/** "All" and the library's categories, the most common first; picking one shows only its podcasts (18.2). */
@Composable
private fun CategoryChips(state: LibraryUiState, actions: LibraryActions, modifier: Modifier = Modifier) {
    val options = listOf(null) + state.categories
    FilterChipRow(
        options = options.map { FilterOption(stringResource(it?.label ?: Res.string.category_all)) },
        selectedIndex = options.indexOf(state.category),
        onSelected = { actions.onCategoryChange(options[it]) },
        modifier = modifier,
    )
}

/** The started episodes in a row above the podcasts, each one played from where it stopped by a tap. */
@Composable
private fun ContinueListening(episodes: List<Episode>, onPlay: (Episode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(bottom = Spacing.s)) {
        SectionTitle(stringResource(Res.string.continue_listening))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            items(items = episodes, key = { it.id }) { episode ->
                val durationMs = episode.duration * AppConfig.MILLIS_PER_SECOND
                ContinueCard(
                    title = episode.title,
                    imageUrl = episode.imageUrl,
                    progress = if (durationMs > 0) episode.playbackPosition.toFloat() / durationMs else 0f,
                    caption = episode.remainingDuration()?.let { stringResource(Res.string.remaining_time, it) },
                    onClick = { onPlay(episode) },
                    onClickLabel = stringResource(Res.string.play),
                )
            }
        }
        SectionTitle(stringResource(Res.string.library_podcasts), Modifier.padding(top = Spacing.m))
    }
}

@Composable
private fun podcastActions(podcast: Podcast, actions: LibraryActions) =
    listOf(ItemAction(stringResource(Res.string.delete_podcast)) { actions.onPodcastLongClick(podcast) })

/** "2 days ago · 3 unplayed", like the episode rows; each half is left out when there is nothing to say. */
@Composable
private fun LibraryEntry.summary(): String? {
    val latest = latestEpisodeDate?.let { relativeTime(it, getCurrentTimestamp())?.text() }
    val unplayed = unplayedCount.takeIf { it > 0 }?.let {
        pluralStringResource(Res.plurals.library_unplayed, it, it)
    }
    return listOfNotNull(latest, unplayed).joinToString(" · ").ifEmpty { null }
}
