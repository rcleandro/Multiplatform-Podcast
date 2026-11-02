package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.core.designsystem.component.ErrorState
import br.com.carvalho.podcast.core.designsystem.component.LoadingState
import br.com.carvalho.podcast.core.designsystem.icons.CloudOff
import br.com.carvalho.podcast.core.designsystem.icons.MyLocation
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.extensions.toTime
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.transcript
import br.com.carvalho.podcast.core.ui.generated.resources.transcript_failed
import br.com.carvalho.podcast.core.ui.generated.resources.transcript_follow
import br.com.carvalho.podcast.core.ui.generated.resources.transcript_no_match
import br.com.carvalho.podcast.core.ui.generated.resources.transcript_search
import br.com.carvalho.podcast.core.ui.generated.resources.try_again
import br.com.carvalho.podcast.domain.model.TranscriptLine
import br.com.carvalho.podcast.presentation.component.SearchField
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The transcript of the episode playing (19.2), over the player. */
@Composable
fun TranscriptScreen(onBack: () -> Unit, viewModel: TranscriptViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    TranscriptContent(state, onBack = onBack, onIntent = viewModel::onIntent)
}

/**
 * The lines follow the audio, the one being said highlighted, until the reader scrolls them; then "Back to what is
 * playing" brings the following back. A search keeps only the lines with its words, each with its time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptContent(
    state: TranscriptUiState,
    onBack: () -> Unit,
    onIntent: (TranscriptIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var following by remember { mutableStateOf(true) }
    val searching = state.query.isNotBlank()
    FollowTheAudio(state, listState, following && !searching)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.transcript), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (!following && !searching && state.current >= 0) {
                ExtendedFloatingActionButton(
                    onClick = { following = true },
                    icon = { Icon(Icons.Rounded.MyLocation, contentDescription = null) },
                    text = { Text(stringResource(Res.string.transcript_follow)) },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query = state.query,
                onQueryChange = { onIntent(TranscriptIntent.ChangeQuery(it)) },
                placeholder = stringResource(Res.string.transcript_search),
            )
            when {
                state.isLoading -> LoadingState()
                state.failed -> ErrorState(
                    icon = Icons.Rounded.CloudOff,
                    title = stringResource(Res.string.transcript_failed),
                    message = null,
                    actionLabel = stringResource(Res.string.try_again),
                    onAction = { onIntent(TranscriptIntent.Retry) },
                )
                searching && state.shown.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.Search,
                    title = stringResource(Res.string.transcript_no_match, state.query.trim()),
                    message = null,
                )
                else -> TranscriptLines(state, listState, onScrolledByHand = { following = false }) {
                    onIntent(TranscriptIntent.PlayFrom(it))
                }
            }
        }
    }
}

/** Keeps the line being said in view while following. */
@Composable
private fun FollowTheAudio(state: TranscriptUiState, listState: LazyListState, following: Boolean) {
    LaunchedEffect(state.current, following, state.lines) {
        if (following && state.current >= 0 && state.current < state.lines.size) {
            listState.animateScrollToItem(state.current)
        }
    }
}

@Composable
private fun TranscriptLines(
    state: TranscriptUiState,
    listState: LazyListState,
    onScrolledByHand: () -> Unit,
    onPlayFrom: (TranscriptLine) -> Unit,
) {
    // A scroll the reader makes stops the following; the list's own scrolling to the line does not come through here.
    val byHand = remember(onScrolledByHand) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) onScrolledByHand()
                return Offset.Zero
            }
        }
    }
    val searching = state.query.isNotBlank()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxSize().nestedScroll(byHand),
    ) {
        items(state.shown, key = { it }) { index ->
            val line = state.lines[index]
            LineRow(line, isCurrent = index == state.current && !searching, showTime = searching) { onPlayFrom(line) }
        }
    }
}

@Composable
private fun LineRow(line: TranscriptLine, isCurrent: Boolean, showTime: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val highlight = if (isCurrent) colors.secondaryContainer else colors.background
    Box(
        modifier = Modifier
            .readableWidth()
            .background(highlight, MaterialTheme.shapes.medium)
            .semantics { selected = isCurrent }
            .clickable(onClick = onClick)
            .padding(Spacing.m),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            val label = listOfNotNull(line.startMs.toTime().takeIf { showTime }, line.speaker).joinToString(" · ")
            if (label.isNotEmpty()) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = PodcastTheme.colors.accentText)
            }
            Text(
                line.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            )
        }
    }
}
