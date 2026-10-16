package br.com.carvalho.podcast.wear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.extensions.toTime
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.audio_output
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.skip_backward
import br.com.carvalho.podcast.core.ui.generated.resources.skip_forward
import br.com.carvalho.podcast.core.ui.generated.resources.volume
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.feature.player.presentation.PlayerIntent
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

/** What the player asks of the watch itself: its volume and where the sound goes (19.9e). */
data class WatchPlayerActions(
    /** One step of the crown or bezel: up or down. */
    val onVolumeStep: (up: Boolean) -> Unit = {},
    val onVolume: () -> Unit = {},
    val onAudioOutput: () -> Unit = {},
)

@Composable
fun NowPlayingScreen(viewModel: PlayerViewModel = koinViewModel()) {
    val state by viewModel.playerState.collectAsState()
    val context = LocalContext.current
    val volume = remember(context) { WatchVolume(context) }
    NowPlayingContent(
        state,
        viewModel::onIntent,
        WatchPlayerActions(
            onVolumeStep = volume::step,
            onVolume = volume::show,
            onAudioOutput = context::chooseAudioOutput,
        ),
    )
}

/**
 * The episode's cover behind everything, darkened so the text reads; its progress along the screen's edge, a ring on
 * a round watch and a rounded square on a square one; title, time, jumps and play; then volume and audio output. The
 * crown or bezel turns the volume, as in the watch's own media player.
 */
@Composable
fun NowPlayingContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    actions: WatchPlayerActions = WatchPlayerActions(),
) {
    val focus = remember { FocusRequester() }
    var turned by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    ScreenScaffold {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                // At most a step per event: a bezel click comes as one large event, a crown's turn as many small
                // ones that add up.
                .onRotaryScrollEvent { event ->
                    turned += event.verticalScrollPixels
                    if (abs(turned) >= ROTARY_STEP_PX) {
                        actions.onVolumeStep(turned > 0)
                        turned = 0f
                    }
                    true
                }
                .focusRequester(focus)
                .focusable(),
        ) {
            Cover(state.currentEpisode?.imageUrl)
            EdgeProgress(state.progress())
            PlayerControls(state, onIntent, actions)
        }
    }
}

@Composable
private fun Cover(imageUrl: String?) {
    val blur = 6.dp
    val colors = MaterialTheme.colorScheme
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().blur(blur),
    )
    Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = COVER_SCRIM_ALPHA)))
}

/**
 * The ring of a round watch, or a rounded square along a square one's edges, read by a screen reader as a progress
 * bar. Wear's ring keeps the first progress function it gets, so it is handed one that reads the latest value.
 */
@Composable
private fun EdgeProgress(progress: Float) {
    val stroke = 4.dp
    val inset = 2.dp
    val colors = MaterialTheme.colorScheme
    val latest by rememberUpdatedState(progress)
    val edge = Modifier
        .fillMaxSize()
        .padding(inset)
        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
    if (LocalConfiguration.current.isScreenRound) {
        CircularProgressIndicator(progress = { latest }, strokeWidth = stroke, modifier = edge)
        return
    }
    val corner = 32.dp
    Canvas(edge) {
        val width = stroke.toPx()
        val edge = Path().apply {
            addRoundRect(
                RoundRect(
                    left = width / 2,
                    top = width / 2,
                    right = size.width - width / 2,
                    bottom = size.height - width / 2,
                    cornerRadius = CornerRadius(corner.toPx()),
                )
            )
        }
        drawPath(edge, colors.surfaceContainerHigh, style = Stroke(width))
        val measure = PathMeasure().apply { setPath(edge, forceClosed = true) }
        val done = Path().also { measure.getSegment(0f, measure.length * progress, it, startWithMoveTo = true) }
        drawPath(done, colors.primary, style = Stroke(width, cap = StrokeCap.Round))
    }
}

@Composable
private fun PlayerControls(state: PlayerState, onIntent: (PlayerIntent) -> Unit, actions: WatchPlayerActions) {
    val playSize = 52.dp
    val skips = state.skipIntervals
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.padding(horizontal = Spacing.xl),
    ) {
        Text(
            state.currentEpisode?.title.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(state.position.toTime(), state.knownDurationMs().takeIf { it > 0 }?.toTime())
                .joinToString(" / "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onIntent(PlayerIntent.SkipBackward) }) {
                Icon(Icons.Rounded.Replay10, stringResource(Res.string.skip_backward, skips.backwardSeconds))
            }
            FilledIconButton(onClick = { onIntent(PlayerIntent.PlayPause) }, modifier = Modifier.size(playSize)) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (state.isPlaying) Res.string.pause else Res.string.play),
                )
            }
            IconButton(onClick = { onIntent(PlayerIntent.SkipForward) }) {
                Icon(Icons.Rounded.Forward30, stringResource(Res.string.skip_forward, skips.forwardSeconds))
            }
        }
        Row {
            IconButton(onClick = actions.onVolume) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(Res.string.volume))
            }
            IconButton(onClick = actions.onAudioOutput) {
                Icon(Icons.Rounded.BluetoothAudio, stringResource(Res.string.audio_output))
            }
        }
    }
}

/** The player's duration once the audio loads; until then, the one the feed gave (seconds). */
private fun PlayerState.knownDurationMs(): Long =
    duration?.takeIf { it > 0 } ?: ((currentEpisode?.duration ?: 0L) * MILLIS_PER_SECOND)

private fun PlayerState.progress(): Float =
    knownDurationMs().takeIf { it > 0 }?.let { (position.toFloat() / it).coerceIn(0f, 1f) } ?: 0f

private const val MILLIS_PER_SECOND = 1000L

/** How much of the cover shows through the dark layer over it. */
private const val COVER_SCRIM_ALPHA = 0.6f

/** Scroll pixels of the crown or bezel per volume step. */
private const val ROTARY_STEP_PX = 40f
