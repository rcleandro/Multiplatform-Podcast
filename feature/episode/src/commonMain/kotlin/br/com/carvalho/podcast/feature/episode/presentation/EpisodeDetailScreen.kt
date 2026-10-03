package br.com.carvalho.podcast.feature.episode.presentation

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.core.designsystem.component.ErrorState
import br.com.carvalho.podcast.core.designsystem.component.HtmlText
import br.com.carvalho.podcast.core.designsystem.component.LoadingState
import br.com.carvalho.podcast.core.designsystem.component.PodcastArtwork
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.description
import br.com.carvalho.podcast.core.ui.generated.resources.episode
import br.com.carvalho.podcast.core.ui.generated.resources.episode_not_found
import br.com.carvalho.podcast.core.ui.generated.resources.error_load_episode
import br.com.carvalho.podcast.core.ui.generated.resources.no_description
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.try_again
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EpisodeDetailScreen(
    episodeId: String,
    viewModel: EpisodeDetailViewModel = koinViewModel(key = episodeId) { parametersOf(episodeId) },
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    EpisodeDetailContent(
        state = uiState,
        onBack = onBackClick,
        onPlay = { viewModel.onIntent(EpisodeDetailIntent.Play) },
        onRetry = { viewModel.onIntent(EpisodeDetailIntent.Retry) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailContent(
    state: EpisodeDetailUiState,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.episode)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets()
    ) { padding ->
        val episode = state.episode
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.loadFailed -> ErrorState(
                icon = Icons.Rounded.CloudOff,
                title = stringResource(Res.string.error_load_episode),
                message = null,
                actionLabel = stringResource(Res.string.try_again),
                onAction = onRetry,
                modifier = Modifier.padding(padding),
            )
            episode == null -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = stringResource(Res.string.episode_not_found),
                message = null,
                modifier = Modifier.padding(padding),
            )
            else -> EpisodeDetailBody(episode, onPlay, Modifier.padding(padding))
        }
    }
}

@Composable
private fun EpisodeDetailBody(episode: Episode, onPlay: () -> Unit, modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.l)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            PodcastArtwork(
                imageUrl = episode.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(Sizes.artworkM),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                episode.podcastTitle?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = PodcastTheme.colors.accentText)
                }
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.s))
            Text(stringResource(Res.string.play))
        }
        Text(text = stringResource(Res.string.description), style = MaterialTheme.typography.titleMedium)
        HtmlText(html = episode.description ?: stringResource(Res.string.no_description))
    }
}
