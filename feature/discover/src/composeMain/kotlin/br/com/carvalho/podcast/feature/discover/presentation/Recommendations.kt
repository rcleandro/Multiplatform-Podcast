package br.com.carvalho.podcast.feature.discover.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.for_you
import br.com.carvalho.podcast.core.ui.generated.resources.similar_none
import br.com.carvalho.podcast.core.ui.generated.resources.similar_to
import org.jetbrains.compose.resources.stringResource

/**
 * Podcasts like one of the library's when asked from its page, else "For you" from the library's categories
 * (19.6), while no category is picked.
 */
@Composable
internal fun Recommendations(state: DiscoverUiState, actions: DiscoverActions) {
    val similar = state.similar
    val (title, results) = when {
        similar != null -> stringResource(Res.string.similar_to, similar.title) to similar.results
        state.category == null && state.forYou.isNotEmpty() -> stringResource(Res.string.for_you) to state.forYou
        else -> return
    }
    ListTitle(title)
    if (results.isEmpty()) {
        Text(
            stringResource(Res.string.similar_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.readableWidth(),
        )
    }
    results.forEach { ResultRow(it, it.podcast.feedUrl in state.subscribing, actions.onSubscribe) }
}
