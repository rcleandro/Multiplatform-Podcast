package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.select_podcast
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailScreen
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailScreen
import org.jetbrains.compose.resources.stringResource

// The detail and extra panes of a tab, each with the view models of its selection (23.10), not the activity's.

@Composable
internal fun PodcastPane(component: RootComponent, podcast: Detail.Podcast?, scope: ViewModelStoreOwner) {
    if (podcast == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.select_podcast), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    WithViewModels(scope) {
        PodcastDetailScreen(
            podcastId = podcast.podcastId,
            onBackClick = component::onBackClicked,
            onEpisodeClick = { id, _ -> component.onEpisodeSelected(id, podcast.podcastId) },
            onSimilarClick = component::onShowSimilar,
        )
    }
}

@Composable
internal fun EpisodePane(component: RootComponent, episode: Detail.Episode?, scope: ViewModelStoreOwner) {
    if (episode == null) return
    WithViewModels(scope) { EpisodeDetailScreen(episodeId = episode.episodeId, onBackClick = component::onBackClicked) }
}
