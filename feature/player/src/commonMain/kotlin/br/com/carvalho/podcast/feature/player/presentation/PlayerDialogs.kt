package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.close
import br.com.carvalho.podcast.core.ui.generated.resources.playback_speed
import br.com.carvalho.podcast.core.ui.generated.resources.queue
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer
import br.com.carvalho.podcast.core.ui.generated.resources.timer_15_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_30_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_45_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_5_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_60_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_disabled
import br.com.carvalho.podcast.core.ui.generated.resources.timer_end_of_episode
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.SleepTimer
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = AppConfig.PLAYBACK_SPEEDS
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.playback_speed)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = speed == currentSpeed,
                                onClick = { onSpeedSelected(speed) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = speed == currentSpeed,
                            onClick = { onSpeedSelected(speed) }
                        )
                        Spacer(modifier = Modifier.width(Spacing.l))
                        Text(
                            text = formatSpeed(speed),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
internal fun SleepTimerDialog(
    playerState: PlayerState,
    onTimerSelected: (SleepTimer?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        null to stringResource(Res.string.timer_disabled),
        SleepTimer.Minutes(5) to stringResource(Res.string.timer_5_min),
        SleepTimer.Minutes(15) to stringResource(Res.string.timer_15_min),
        SleepTimer.Minutes(30) to stringResource(Res.string.timer_30_min),
        SleepTimer.Minutes(45) to stringResource(Res.string.timer_45_min),
        SleepTimer.Minutes(60) to stringResource(Res.string.timer_60_min),
        SleepTimer.EndOfEpisode to stringResource(Res.string.timer_end_of_episode),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sleep_timer)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                options.forEach { (timer, label) ->
                    val isSelected = timer == playerState.sleepTimer

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { onTimerSelected(timer) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onTimerSelected(timer) }
                        )
                        Spacer(modifier = Modifier.width(Spacing.l))
                        Text(
                            text = if (isSelected && timer is SleepTimer.Minutes) {
                                "$label (${formatRemainingTime(playerState.sleepTimerMillis)})"
                            } else {
                                label
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
        }
    )
}

@Composable
internal fun QueueDialog(
    queue: List<Episode>,
    currentEpisodeId: String?,
    onEpisodeSelected: (Episode) -> Unit,
    onDismiss: () -> Unit
) {
    val maxQueueHeight = 400.dp
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.queue)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxQueueHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                queue.forEach { episode ->
                    val isCurrent = episode.id == currentEpisodeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEpisodeSelected(episode) }
                            .padding(vertical = Spacing.s),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCurrent) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(Spacing.s))
                        }
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrent) {
                                PodcastTheme.colors.accentText
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
        }
    )
}

private fun formatRemainingTime(millis: Long?): String {
    if (millis == null) return ""
    val totalSeconds = millis / AppConfig.MILLIS_PER_SECOND
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** "1×", "1.5×", "0.8×": no trailing ".0". */
internal fun formatSpeed(speed: Float): String = speed.toString().removeSuffix(".0") + "×"

private const val SECONDS_PER_MINUTE = 60
