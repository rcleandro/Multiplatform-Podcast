package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.core.designsystem.LocalTabletopFold
import br.com.carvalho.podcast.core.designsystem.currentWindowIsShort
import br.com.carvalho.podcast.core.designsystem.currentPaneLayout
import br.com.carvalho.podcast.core.designsystem.PaneLayout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import br.com.carvalho.podcast.core.ui.generated.resources.state_on
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer_short
import br.com.carvalho.podcast.core.ui.generated.resources.queue_short
import br.com.carvalho.podcast.core.designsystem.component.morphingShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.carvalho.podcast.core.AppConfig
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
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.minimize
import br.com.carvalho.podcast.core.ui.generated.resources.next
import br.com.carvalho.podcast.core.ui.generated.resources.now_playing
import br.com.carvalho.podcast.core.ui.generated.resources.playback_speed
import br.com.carvalho.podcast.core.ui.generated.resources.previous
import br.com.carvalho.podcast.core.ui.generated.resources.queue
import br.com.carvalho.podcast.core.ui.generated.resources.skip_backward
import br.com.carvalho.podcast.core.ui.generated.resources.skip_forward
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer
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
fun PlayerContent(
    state: PlayerState,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
) {
    ArtworkBackdrop(imageUrl = state.currentEpisode?.imageUrl, modifier = modifier.fillMaxSize()) {
        Column {
            PodcastTopBar(
                navigationIcon = Icons.Rounded.KeyboardArrowDown,
                navigationContentDescription = stringResource(Res.string.minimize),
                onNavigate = actions.onMinimize,
                title = stringResource(Res.string.now_playing),
            )
            val isShort = currentWindowIsShort()
            val paneLayout = currentPaneLayout()
            val fold = LocalTabletopFold.current
            if (fold != null) {
                TabletopPlayer(state, actions, fold, artworkModifier)
            } else if (isShort && paneLayout == PaneLayout.COMPACT) {
                CoverScreenPlayer(state, actions)
            } else if (paneLayout == PaneLayout.EXPANDED || isShort) {
                // A wide or a short window puts the cover beside the rest instead of above it (21.1, 21.2), so
                // the play button is in view without scrolling.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xxl),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.xl, vertical = if (isShort) Spacing.s else Spacing.l),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PlayerArtwork(state.currentEpisode, artworkModifier)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // Tighter on a short window, so speed, queue and timer fit under the controls too.
                        verticalArrangement = Arrangement.spacedBy(if (isShort) Spacing.m else Spacing.xl),
                        modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    ) {
                        PlayerInfoAndControls(state, actions)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.xl, vertical = Spacing.l),
                ) {
                    PlayerArtwork(state.currentEpisode, artworkModifier)
                    PlayerInfoAndControls(state, actions)
                }
            }
        }
    }
}

/** Title, progress and controls: under the cover on a phone, beside it on a wide window. */
@Composable
internal fun PlayerInfoAndControls(state: PlayerState, actions: PlayerActions, showTitle: Boolean = true) {
    if (showTitle) PlayerTitle(state.currentEpisode)
    PlayerSlider(
        positionMs = state.position,
        durationMs = state.knownDurationMs(),
        onSeek = actions.onSeek,
        formatTime = { it.toTime() },
        wavy = state.isPlaying,
    )
    PlayerControls(state, actions)
    PlayerAuxRow(state, actions)
}

private const val MS_PER_SECOND = 1000L

/** The player's duration once the audio loads; until then, the one the feed gave (seconds). 0 when neither knows. */
private fun PlayerState.knownDurationMs(): Long =
    duration?.takeIf { it > 0 } ?: ((currentEpisode?.duration ?: 0L) * MS_PER_SECOND)

@Composable
internal fun PlayerArtwork(episode: Episode?, artworkModifier: Modifier) {
    // The cover takes the phone's width (24.2), up to a size that still leaves the controls in view on a tablet.
    val artworkMax = 360.dp
    PodcastArtwork(
        imageUrl = episode?.imageUrl,
        contentDescription = episode?.title,
        shape = MaterialTheme.shapes.extraLarge,
        // Beside the controls the height bounds it too, so a low window shows the whole cover.
        modifier = artworkModifier
            .widthIn(max = artworkMax)
            .fillMaxWidth()
            .aspectRatio(1f, matchHeightConstraintsFirst = true),
    )
}

@Composable
private fun PlayerControls(state: PlayerState, actions: PlayerActions) {
    val currentIndex = state.queue.indexOfFirst { it.id == state.currentEpisode?.id }
    val playButtonSize = 80.dp
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        ControlButton(
            icon = Icons.Rounded.SkipPrevious,
            label = stringResource(Res.string.previous),
            onClick = actions.onPrevious,
            enabled = currentIndex > 0,
        )
        ControlButton(
            icon = skipBackwardIcon(AppConfig.SKIP_BACKWARD_SECONDS),
            label = stringResource(Res.string.skip_backward, AppConfig.SKIP_BACKWARD_SECONDS),
            onClick = actions.onSkipBackward,
        )
        // A square with large corners while playing, a circle when paused (24.2).
        PlayPauseButton(
            isPlaying = state.isPlaying,
            isLoading = state.isBuffering,
            onClick = actions.onPlayPause,
            size = playButtonSize,
            shape = morphingShape(round = !state.isPlaying),
        )
        ControlButton(
            icon = skipForwardIcon(AppConfig.SKIP_FORWARD_SECONDS),
            label = stringResource(Res.string.skip_forward, AppConfig.SKIP_FORWARD_SECONDS),
            onClick = actions.onSkipForward,
        )
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        AuxButton(
            icon = Icons.Rounded.Speed,
            label = formatSpeed(state.speed),
            description = stringResource(Res.string.playback_speed),
            active = state.speed != 1f,
            onClick = actions.onSpeedClick,
        )
        AuxButton(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            label = stringResource(Res.string.queue_short),
            description = stringResource(Res.string.queue),
            active = false,
            onClick = actions.onQueueClick,
        )
        AuxButton(
            icon = Icons.Rounded.Timer,
            label = stringResource(Res.string.sleep_timer_short),
            description = stringResource(Res.string.sleep_timer),
            active = state.sleepTimer != null,
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

/** Secondary player action: a tonal pill with icon and short label, amber while the setting is on. */
@Composable
private fun AuxButton(icon: ImageVector, label: String, description: String?, active: Boolean, onClick: () -> Unit) {
    // Color alone does not tell a screen reader user that the setting is on.
    val onLabel = stringResource(Res.string.state_on)
    FilledTonalButton(
        onClick = onClick,
        colors = if (active) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        },
        modifier = Modifier.semantics { if (active) stateDescription = onLabel },
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(Sizes.iconS))
        Spacer(modifier = Modifier.width(Spacing.s))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** The icon with the jump's number when Material has one (5, 10, 30 s); a plain arrow otherwise. */
internal fun skipForwardIcon(seconds: Int): ImageVector = when (seconds) {
    SHORT_JUMP -> Icons.Rounded.Forward5
    MEDIUM_JUMP -> Icons.Rounded.Forward10
    LONG_JUMP -> Icons.Rounded.Forward30
    else -> Icons.Rounded.FastForward
}

internal fun skipBackwardIcon(seconds: Int): ImageVector = when (seconds) {
    SHORT_JUMP -> Icons.Rounded.Replay5
    MEDIUM_JUMP -> Icons.Rounded.Replay10
    LONG_JUMP -> Icons.Rounded.Replay30
    else -> Icons.Rounded.FastRewind
}

private const val SHORT_JUMP = 5
private const val MEDIUM_JUMP = 10
private const val LONG_JUMP = 30
