package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
@Composable
fun PlayerSlider(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
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
            colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
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
