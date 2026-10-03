package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ArtworkBackdrop
import br.com.carvalho.podcast.core.designsystem.component.PlayPauseButton
import br.com.carvalho.podcast.core.designsystem.component.PlayerSlider
import br.com.carvalho.podcast.core.designsystem.component.PodcastArtwork
import br.com.carvalho.podcast.core.designsystem.component.PodcastTopBar
import br.com.carvalho.podcast.core.extensions.toTime
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.minimize
import br.com.carvalho.podcast.shared.next
import br.com.carvalho.podcast.shared.no_episode_selected
import br.com.carvalho.podcast.shared.now_playing
import br.com.carvalho.podcast.shared.playback_speed
import br.com.carvalho.podcast.shared.previous
import br.com.carvalho.podcast.shared.queue
import br.com.carvalho.podcast.shared.skip_backward
import br.com.carvalho.podcast.shared.skip_forward
import br.com.carvalho.podcast.shared.sleep_timer
import org.jetbrains.compose.resources.stringResource

/** Everything the player can ask for; the screen wires these to the view model and its dialogs. */
data class PlayerActions(
    val onMinimize: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onSeek: (Long) -> Unit = {},
    val onSkipBackward: () -> Unit = {},
    val onSkipForward: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onSpeedClick: () -> Unit = {},
    val onQueueClick: () -> Unit = {},
    val onSleepTimerClick: () -> Unit = {},
)

/** Stateless full-screen player. */
@Composable
fun PlayerContent(state: PlayerState, actions: PlayerActions, modifier: Modifier = Modifier) {
    ArtworkBackdrop(imageUrl = state.currentEpisode?.imageUrl, modifier = modifier.fillMaxSize()) {
        Column {
            PodcastTopBar(
                navigationIcon = Icons.Rounded.KeyboardArrowDown,
                navigationContentDescription = stringResource(Res.string.minimize),
                onNavigate = actions.onMinimize,
                title = stringResource(Res.string.now_playing),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.l),
            ) {
                PlayerHeader(state.currentEpisode)
                PlayerSlider(
                    positionMs = state.position,
                    durationMs = state.duration ?: 0L,
                    onSeek = actions.onSeek,
                    formatTime = { it.toTime() },
                )
                PlayerControls(state, actions)
                PlayerAuxRow(state, actions)
            }
        }
    }
}

@Composable
private fun PlayerHeader(episode: Episode?) {
    PodcastArtwork(
        imageUrl = episode?.imageUrl,
        contentDescription = episode?.title,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.widthIn(max = Sizes.artworkL).fillMaxWidth().aspectRatio(1f),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = episode?.title ?: stringResource(Res.string.no_episode_selected),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        episode?.podcastTitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = PodcastTheme.colors.accentText,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlayerControls(state: PlayerState, actions: PlayerActions) {
    val currentIndex = state.queue.indexOfFirst { it.id == state.currentEpisode?.id }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        ControlButton(
            icon = Icons.Rounded.SkipPrevious,
            label = stringResource(Res.string.previous),
            onClick = actions.onPrevious,
            enabled = currentIndex > 0,
        )
        ControlButton(Icons.Rounded.Replay10, stringResource(Res.string.skip_backward), actions.onSkipBackward)
        PlayPauseButton(
            isPlaying = state.isPlaying,
            isLoading = state.isBuffering,
            onClick = actions.onPlayPause,
            size = Sizes.playButtonLarge,
        )
        ControlButton(Icons.Rounded.Forward30, stringResource(Res.string.skip_forward), actions.onSkipForward)
        ControlButton(
            icon = Icons.Rounded.SkipNext,
            label = stringResource(Res.string.next),
            onClick = actions.onNext,
            enabled = currentIndex != -1 && currentIndex < state.queue.lastIndex,
        )
    }
}

@Composable
private fun PlayerAuxRow(state: PlayerState, actions: PlayerActions) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        AuxButton(
            icon = Icons.Rounded.Speed,
            label = formatSpeed(state.speed),
            description = stringResource(Res.string.playback_speed),
            active = state.speed != 1f,
            onClick = actions.onSpeedClick,
        )
        AuxButton(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            label = stringResource(Res.string.queue),
            description = null,
            active = false,
            onClick = actions.onQueueClick,
        )
        AuxButton(
            icon = Icons.Rounded.Timer,
            label = stringResource(Res.string.sleep_timer),
            description = null,
            active = state.sleepTimerMillis != null,
            onClick = actions.onSleepTimerClick,
        )
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(Sizes.touchTarget)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(Sizes.iconL))
    }
}

/** Secondary player action: icon over a short label; amber while the setting is on. */
@Composable
private fun AuxButton(icon: ImageVector, label: String, description: String?, active: Boolean, onClick: () -> Unit) {
    val color = if (active) PodcastTheme.colors.accentText else MaterialTheme.colorScheme.onSurfaceVariant
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = description, tint = color, modifier = Modifier.size(Sizes.iconM))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
