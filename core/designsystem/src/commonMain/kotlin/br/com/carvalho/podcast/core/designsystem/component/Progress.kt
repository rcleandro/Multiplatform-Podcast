package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.math.sin
import kotlin.math.PI
import br.com.carvalho.podcast.core.designsystem.Motion
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.generated.resources.Res
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_position_of_duration
import org.jetbrains.compose.resources.stringResource

/** Thin progress line for rows and the mini player. [progress] is 0..1. */
@Composable
fun EpisodeProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val height = 3.dp
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(height),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        drawStopIndicator = {},
        gapSize = 0.dp,
    )
}

/**
 * Seek bar of the player with elapsed and remaining time. The position follows the finger while dragging and
 * [onSeek] fires once on release. Screen readers hear "12:05 of 47:30".
 */
@OptIn(ExperimentalMaterial3Api::class) // the slider's track slot, for the wavy track
@Composable
fun PlayerSlider(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
    wavy: Boolean = false,
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging?.toLong() ?: positionMs
    // Without a duration there is nothing to measure against: an empty bar, not a full one at "−0:00".
    val known = durationMs > 0
    val description = stringResource(Res.string.ds_position_of_duration, formatTime(shown), formatTime(durationMs))

    Column(modifier = modifier) {
        Slider(
            value = if (known) shown.toFloat() else 0f,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            track = { sliderState ->
                val range = sliderState.valueRange
                val fraction = (sliderState.value - range.start) / (range.endInclusive - range.start)
                WavyTrack(fraction = if (known) fraction else 0f, wavy = wavy)
            },
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = description },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatTime(shown),
                style = PodcastTheme.typography.timer,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (known) {
                Text(
                    text = "−" + formatTime((durationMs - shown).coerceAtLeast(0L)),
                    style = PodcastTheme.typography.timer,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val WAVE_PERIOD_MS = 1_600
private const val FULL_TURN = 2 * PI.toFloat()

/**
 * The seek bar's track: a wave on the part already heard while [wavy] (playing), flattening to a line when paused,
 * the wavy progress of Material 3 Expressive drawn by hand (ADR 0001, "Layout das telas").
 */
@Composable
private fun WavyTrack(fraction: Float, wavy: Boolean) {
    val height = 16.dp
    val stroke = 4.dp
    val wavelength = 28.dp
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.surfaceContainerHighest
    val amplitude by animateDpAsState(if (wavy) 3.dp else 0.dp, tween(Motion.MEDIUM, easing = Motion.Standard))
    // The wave travels only while it is there; a paused bar does not keep a frame clock running.
    val phase = if (amplitude > 0.dp) {
        val travel by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(WAVE_PERIOD_MS, easing = LinearEasing)),
        )
        travel
    } else {
        0f
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
        val centerY = size.height / 2
        val split = size.width * fraction.coerceIn(0f, 1f)
        val strokePx = stroke.toPx()
        drawLine(inactive, Offset(split, centerY), Offset(size.width, centerY), strokePx, StrokeCap.Round)
        val amplitudePx = amplitude.toPx()
        val wavelengthPx = wavelength.toPx()
        val wave = Path()
        var x = 0f
        while (x <= split) {
            val y = centerY + amplitudePx * sin(FULL_TURN * (x / wavelengthPx - phase))
            if (x == 0f) wave.moveTo(x, y) else wave.lineTo(x, y)
            x += 1f
        }
        drawPath(wave, active, style = Stroke(width = strokePx, cap = StrokeCap.Round))
    }
}
