package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.LocalPictureInPicture
import br.com.carvalho.podcast.core.designsystem.LocalTabletopFold
import br.com.carvalho.podcast.core.designsystem.PaneLayout
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ArtworkBackdrop
import br.com.carvalho.podcast.core.designsystem.component.PlayPauseButton
import br.com.carvalho.podcast.core.designsystem.component.PlayerSlider
import br.com.carvalho.podcast.core.designsystem.component.PodcastArtwork
import br.com.carvalho.podcast.core.designsystem.component.PodcastTopBar
import br.com.carvalho.podcast.core.designsystem.component.morphingShape
import br.com.carvalho.podcast.core.designsystem.currentPaneLayout
import br.com.carvalho.podcast.core.designsystem.currentWindowIsShort
import br.com.carvalho.podcast.core.extensions.toTime
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.bookmarks
import br.com.carvalho.podcast.core.ui.generated.resources.chapters
import br.com.carvalho.podcast.core.ui.generated.resources.minimize
import br.com.carvalho.podcast.core.ui.generated.resources.next
import br.com.carvalho.podcast.core.ui.generated.resources.now_playing
import br.com.carvalho.podcast.core.ui.generated.resources.playback_speed
import br.com.carvalho.podcast.core.ui.generated.resources.previous
import br.com.carvalho.podcast.core.ui.generated.resources.queue
import br.com.carvalho.podcast.core.ui.generated.resources.queue_short
import br.com.carvalho.podcast.core.ui.generated.resources.share
import br.com.carvalho.podcast.core.ui.generated.resources.skip_backward
import br.com.carvalho.podcast.core.ui.generated.resources.skip_forward
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer_extend
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer_extend_short
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer_short
import br.com.carvalho.podcast.core.ui.generated.resources.state_on
import br.com.carvalho.podcast.core.ui.generated.resources.transcript
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.presentation.format.formatSpeed
import org.jetbrains.compose.resources.stringResource

/** Everything the player can ask for; the screen wires these to the view model and its dialogs. */
data class PlayerActions(
    val onMinimize: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onSeek: (Long) -> Unit = {},
    val onSkipBackward: () -> Unit = {},
    val onSkipForward: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onSpeedClick: () -> Unit = {},
    val onQueueClick: () -> Unit = {},
    val onSleepTimerClick: () -> Unit = {},
    val onExtendSleepTimer: () -> Unit = {},
    val onChaptersClick: () -> Unit = {},
    val onBookmarksClick: () -> Unit = {},
    val onTranscriptClick: () -> Unit = {},
)

/** Shows the platform's video at the size the modifier gives it (19.10); `null` where the platform shows none. */
typealias VideoSurface = @Composable (Modifier) -> Unit

private const val VIDEO_ASPECT_RATIO = 16f / 9f

/** Stateless full-screen player. */
@Composable
fun PlayerContent(
    state: PlayerState,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
    videoSurface: VideoSurface? = null,
) {
    // Shrunk to picture-in-picture, the window has room for the video alone.
    val video = videoSurface?.takeIf { state.currentEpisode?.isVideo == true }
    if (video != null && LocalPictureInPicture.current) {
        video(modifier.fillMaxSize())
        return
    }
    ArtworkBackdrop(imageUrl = state.currentEpisode?.imageUrl, modifier = modifier.fillMaxSize()) {
        Column {
            PodcastTopBar(
                navigationIcon = Icons.Rounded.KeyboardArrowDown,
                navigationContentDescription = stringResource(Res.string.minimize),
                onNavigate = actions.onMinimize,
                title = stringResource(Res.string.now_playing),
                actions = {
                    if (state.currentEpisode != null) {
                        CastButton()
                        IconButton(onClick = actions.onShare) {
                            Icon(Icons.Rounded.Share, contentDescription = stringResource(Res.string.share))
                        }
                    }
                },
            )
            val isShort = currentWindowIsShort()
            val paneLayout = currentPaneLayout()
            val fold = LocalTabletopFold.current
            if (fold != null) {
                TabletopPlayer(state, actions, fold, artworkModifier, video)
            } else if (isShort && paneLayout == PaneLayout.COMPACT) {
                CoverScreenPlayer(state, actions)
            } else if (paneLayout != PaneLayout.COMPACT || isShort) {
                // Wider than a phone held upright, or low: the cover beside the rest instead of above it (21.1,
                // 21.2), so the play button is in view without scrolling.
                SideBySidePlayer(state, actions, artworkModifier, isShort, video)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.xl, vertical = Spacing.l),
                ) {
                    PlayerArtwork(state.currentEpisode, artworkModifier, video)
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
    state.currentChapter?.let { chapter ->
        Text(
            text = chapterLabel(chapter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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

/** The cover, or the video in its place when the episode has one and the platform shows it (19.10). */
@Composable
internal fun PlayerArtwork(episode: Episode?, artworkModifier: Modifier, video: VideoSurface? = null) {
    if (video != null) {
        // Wider than the cover, as videos are; the height bounds it the same way.
        val videoMax = 640.dp
        video(
            artworkModifier
                .widthIn(max = videoMax)
                .fillMaxWidth()
                .aspectRatio(VIDEO_ASPECT_RATIO, matchHeightConstraintsFirst = true),
        )
        return
    }
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
    val playButtonSize = 80.dp
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        ControlButton(
            icon = Icons.Rounded.SkipPrevious,
            label = stringResource(Res.string.previous),
            onClick = actions.onPrevious,
            enabled = state.currentEpisode != null,
        )
        ControlButton(
            icon = skipBackwardIcon(state.skipIntervals.backwardSeconds),
            label = stringResource(Res.string.skip_backward, state.skipIntervals.backwardSeconds),
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
            icon = skipForwardIcon(state.skipIntervals.forwardSeconds),
            label = stringResource(Res.string.skip_forward, state.skipIntervals.forwardSeconds),
            onClick = actions.onSkipForward,
        )
        ControlButton(
            icon = Icons.Rounded.SkipNext,
            label = stringResource(Res.string.next),
            onClick = actions.onNext,
            enabled = state.queue.isNotEmpty(),
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
        EpisodeAuxButtons(state, actions)
        // While minutes run down, the button shows what is left and "+5 min" sits beside it (18.15).
        val remaining = state.sleepTimerMillis?.takeIf { state.sleepTimer is SleepTimer.Minutes }
        AuxButton(
            icon = Icons.Rounded.Bedtime,
            label = remaining?.let(::formatRemainingTime) ?: stringResource(Res.string.sleep_timer_short),
            description = stringResource(Res.string.sleep_timer),
            active = state.sleepTimer != null,
            onClick = actions.onSleepTimerClick,
        )
        if (remaining != null) {
            AuxButton(
                icon = Icons.Rounded.MoreTime,
                label = stringResource(Res.string.sleep_timer_extend_short),
                description = stringResource(Res.string.sleep_timer_extend),
                active = false,
                onClick = actions.onExtendSleepTimer,
            )
        }
    }
}

/** What the episode playing has: bookmarks (19.3), a transcript (19.2) and chapters (19.1). */
@Composable
private fun EpisodeAuxButtons(state: PlayerState, actions: PlayerActions) {
    val episode = state.currentEpisode ?: return
    AuxButton(Icons.Rounded.EditNote, stringResource(Res.string.bookmarks), null, false, actions.onBookmarksClick)
    if (episode.transcriptUrl != null) {
        AuxButton(
            Icons.Rounded.Subtitles,
            stringResource(Res.string.transcript),
            null,
            false,
            actions.onTranscriptClick,
        )
    }
    if (state.chapters.isNotEmpty()) {
        AuxButton(
            Icons.AutoMirrored.Rounded.FormatListBulleted,
            stringResource(Res.string.chapters),
            null,
            false,
            actions.onChaptersClick,
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
