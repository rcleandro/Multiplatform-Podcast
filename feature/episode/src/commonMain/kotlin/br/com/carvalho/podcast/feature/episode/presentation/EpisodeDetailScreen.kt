package br.com.carvalho.podcast.feature.episode.presentation

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import br.com.carvalho.podcast.core.designsystem.component.morphingShape
import br.com.carvalho.podcast.core.designsystem.component.SectionTitle
import br.com.carvalho.podcast.core.designsystem.component.DownloadState
import br.com.carvalho.podcast.core.designsystem.component.ArtworkBackdrop
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.FlowRow
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.resume_remaining
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.presentation.component.toDownloadState
import br.com.carvalho.podcast.presentation.component.remainingDuration
import br.com.carvalho.podcast.presentation.component.downloadAction
import br.com.carvalho.podcast.presentation.format.text
import br.com.carvalho.podcast.presentation.format.relativeTime
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.rounded.Pause
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
        actions = EpisodeDetailActions(
            onBack = onBackClick,
            onPlayPause = { viewModel.onIntent(EpisodeDetailIntent.PlayPause) },
            onRetry = { viewModel.onIntent(EpisodeDetailIntent.Retry) },
            onDownload = { viewModel.onIntent(EpisodeDetailIntent.Download) },
            onCancelDownload = { viewModel.onIntent(EpisodeDetailIntent.CancelDownload) },
            onDeleteDownload = { viewModel.onIntent(EpisodeDetailIntent.DeleteDownload) },
            onMarkPlayed = { viewModel.onIntent(EpisodeDetailIntent.MarkPlayed) },
            onMarkUnplayed = { viewModel.onIntent(EpisodeDetailIntent.MarkUnplayed) },
        ),
    )
}

data class EpisodeDetailActions(
    val onBack: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onDownload: () -> Unit = {},
    val onCancelDownload: () -> Unit = {},
    val onDeleteDownload: () -> Unit = {},
    val onMarkPlayed: () -> Unit = {},
    val onMarkUnplayed: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailContent(
    state: EpisodeDetailUiState,
    actions: EpisodeDetailActions,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Clear over the tinted header, like the podcast's; it takes a color once the page scrolls under it.
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
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
                onAction = actions.onRetry,
                modifier = Modifier.padding(padding),
            )
            episode == null -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = stringResource(Res.string.episode_not_found),
                message = null,
                modifier = Modifier.padding(padding),
            )
            else -> EpisodeDetailBody(episode, state, actions, padding.calculateTopPadding())
        }
    }
}

@Composable
private fun EpisodeDetailBody(
    episode: Episode,
    state: EpisodeDetailUiState,
    actions: EpisodeDetailActions,
    topInset: Dp,
) {
    val artworkSize = 200.dp
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // The cover centered over a background tinted by it, the same header as the podcast's (24.2).
        ArtworkBackdrop(imageUrl = episode.imageUrl, modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
                modifier = Modifier
                    .padding(horizontal = Spacing.l)
                    .padding(top = topInset + Spacing.m, bottom = Spacing.l),
            ) {
                PodcastArtwork(
                    imageUrl = episode.imageUrl,
                    contentDescription = null,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(artworkSize),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    episode.podcastTitle?.let {
                        Text(it, style = MaterialTheme.typography.labelLarge, color = PodcastTheme.colors.accentText)
                    }
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    EpisodeFacts(episode)
                }
                PlayPauseButton(episode, state, actions.onPlayPause)
                EpisodeActionButtons(episode, state, actions)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            // The end of the description stays clear of the mini player.
            modifier = Modifier.padding(start = Spacing.l, end = Spacing.l, bottom = Sizes.listBottomInset),
        ) {
            SectionTitle(stringResource(Res.string.description))
            HtmlText(html = episode.description ?: stringResource(Res.string.no_description))
        }
    }
}

/** When it came out and how long it is; what is left goes on the play button. */
@Composable
private fun EpisodeFacts(episode: Episode) {
    val published = relativeTime(episode.publishDate, getCurrentTimestamp())?.text()
    val length = episode.duration.takeIf { it > 0 }?.toDuration()
    listOfNotNull(published, length).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "Play" for a new episode, "Resume · 42min left" for a started one, "Pause" while it plays. */
@Composable
private fun PlayPauseButton(episode: Episode, state: EpisodeDetailUiState, onClick: () -> Unit) {
    val remaining = episode.remainingDuration()
    val label = when {
        state.isPlaying -> stringResource(Res.string.pause)
        remaining != null -> stringResource(Res.string.resume_remaining, remaining)
        else -> stringResource(Res.string.play)
    }
    // Rounds while pressed: the shape change of the visual reference.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Button(
        onClick = onClick,
        shape = morphingShape(round = pressed),
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget),
    ) {
        Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(Spacing.s))
        Text(label)
    }
}

/** The download action of its state and mark as played or unplayed: what the rows keep in their "⋮" menu. */
@Composable
private fun EpisodeActionButtons(episode: Episode, state: EpisodeDetailUiState, actions: EpisodeDetailActions) {
    // Each button as wide as its label; they wrap instead of squeezing a label onto two lines.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (supportsDownloads) {
            val downloadState = state.downloadStatus.toDownloadState(episode.isDownloaded)
            val download = downloadAction(
                downloadState,
                actions.onDownload,
                actions.onCancelDownload,
                actions.onDeleteDownload,
            )
            IconLabelButton(downloadState.icon(), download.label, download.onClick)
        }
        if (episode.isPlayed) {
            IconLabelButton(
                Icons.Rounded.RemoveDone,
                stringResource(Res.string.mark_as_unplayed),
                actions.onMarkUnplayed,
            )
        } else {
            IconLabelButton(Icons.Rounded.Done, stringResource(Res.string.mark_as_played), actions.onMarkPlayed)
        }
    }
}

@Composable
private fun IconLabelButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.iconS))
        Spacer(modifier = Modifier.width(Spacing.s))
        Text(label)
    }
}

private fun DownloadState.icon(): ImageVector = when (this) {
    DownloadState.Idle -> Icons.Rounded.Download
    DownloadState.Queued, is DownloadState.Downloading -> Icons.Rounded.Close
    DownloadState.Downloaded -> Icons.Rounded.Delete
    DownloadState.Failed -> Icons.Rounded.Refresh
}
