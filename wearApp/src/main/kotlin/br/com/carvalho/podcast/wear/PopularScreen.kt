package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.popular
import br.com.carvalho.podcast.core.ui.generated.resources.subscribe
import br.com.carvalho.podcast.core.ui.generated.resources.subscribed
import br.com.carvalho.podcast.core.ui.generated.resources.subscribing
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import br.com.carvalho.podcast.feature.discover.presentation.DiscoverIntent
import br.com.carvalho.podcast.feature.discover.presentation.DiscoverViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The most popular podcasts, subscribed to with a tap: on a watch alone there is no keyboard for a feed address, and
 * subscriptions from the phone come with 19.9b.
 */
@Composable
fun PopularScreen(viewModel: DiscoverViewModel = koinViewModel(key = POPULAR_KEY)) {
    val state by viewModel.uiState.collectAsState()
    PopularContent(
        state.results,
        subscribing = state.subscribing,
        onSubscribe = { viewModel.onIntent(DiscoverIntent.Subscribe(it)) },
    )
}

@Composable
fun PopularContent(
    results: List<DirectoryResult>,
    onSubscribe: (DirectoryResult) -> Unit,
    subscribing: Set<String> = emptySet(),
) {
    val progressSize = 32.dp
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(Res.string.popular)) } }
            items(results, key = { it.podcast.feedUrl }) { result ->
                // While the feed is read, a spinner over the cover and "Subscribing…": a tap must show it was heard.
                val isSubscribing = result.podcast.feedUrl in subscribing
                Button(
                    onClick = { onSubscribe(result) },
                    icon = {
                        if (isSubscribing) {
                            CircularProgressIndicator(modifier = Modifier.size(progressSize))
                        } else {
                            Thumbnail(result.podcast.artworkUrl)
                        }
                    },
                    label = { Text(result.podcast.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    secondaryLabel = {
                        Text(
                            stringResource(
                                when {
                                    isSubscribing -> Res.string.subscribing
                                    result.isSubscribed -> Res.string.subscribed
                                    else -> Res.string.subscribe
                                }
                            )
                        )
                    },
                    colors = if (result.isSubscribed) {
                        ButtonDefaults.outlinedButtonColors()
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private const val POPULAR_KEY = "wear-popular"
