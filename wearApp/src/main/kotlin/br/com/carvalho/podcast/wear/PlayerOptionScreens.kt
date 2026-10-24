package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.icons.GraphicEq
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.playback_speed
import br.com.carvalho.podcast.core.ui.generated.resources.queue
import br.com.carvalho.podcast.core.ui.generated.resources.queue_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer
import br.com.carvalho.podcast.core.ui.generated.resources.timer_15_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_30_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_45_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_5_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_60_min
import br.com.carvalho.podcast.core.ui.generated.resources.timer_disabled
import br.com.carvalho.podcast.core.ui.generated.resources.timer_end_of_episode
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.feature.player.presentation.PlayerIntent
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.player.presentation.QueueIntent
import br.com.carvalho.podcast.feature.player.presentation.QueueViewModel
import br.com.carvalho.podcast.presentation.format.formatSpeed
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The speeds of the phone's player (19.9h); picking one goes back to the player. */
@Composable
fun SpeedScreen(onDone: () -> Unit, viewModel: PlayerViewModel = koinViewModel()) {
    val state by viewModel.playerState.collectAsState()
    ChoiceList(
        title = stringResource(Res.string.playback_speed),
        choices = AppConfig.PLAYBACK_SPEEDS.map { formatSpeed(it) to (it == state.speed) },
        onChoose = { index ->
            viewModel.onIntent(PlayerIntent.SetSpeed(AppConfig.PLAYBACK_SPEEDS[index]))
            onDone()
        },
    )
}

/** The sleep timer's choices, as on the phone (19.9h). */
@Suppress("MagicNumber") // the minutes are the choices themselves
internal val SLEEP_TIMER_CHOICES: List<Pair<SleepTimer?, StringResource>> = listOf(
    null to Res.string.timer_disabled,
    SleepTimer.Minutes(5) to Res.string.timer_5_min,
    SleepTimer.Minutes(15) to Res.string.timer_15_min,
    SleepTimer.Minutes(30) to Res.string.timer_30_min,
    SleepTimer.Minutes(45) to Res.string.timer_45_min,
    SleepTimer.Minutes(60) to Res.string.timer_60_min,
    SleepTimer.EndOfEpisode to Res.string.timer_end_of_episode,
)

@Composable
fun SleepTimerScreen(onDone: () -> Unit, viewModel: PlayerViewModel = koinViewModel()) {
    val state by viewModel.playerState.collectAsState()
    ChoiceList(
        title = stringResource(Res.string.sleep_timer),
        choices = SLEEP_TIMER_CHOICES.map { (timer, label) -> stringResource(label) to (timer == state.sleepTimer) },
        onChoose = { index ->
            viewModel.onIntent(PlayerIntent.SetSleepTimer(SLEEP_TIMER_CHOICES[index].first))
            onDone()
        },
    )
}

/** One choice of several, the current one checked. */
@Composable
internal fun ChoiceList(title: String, choices: List<Pair<String, Boolean>>, onChoose: (Int) -> Unit) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(title) } }
            items(choices.size) { index ->
                val (label, chosen) = choices[index]
                Button(
                    onClick = { onChoose(index) },
                    label = { Text(label) },
                    icon = if (chosen) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    colors = if (chosen) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * The phone's queue on the watch (19.9h): what plays and what comes next. A tap plays an episode; a long press takes
 * it out of the queue.
 */
@Composable
fun QueueScreen(viewModel: QueueViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    QueueContent(
        current = state.current,
        isPlaying = state.isPlaying,
        upNext = state.upNext,
        onPlay = { viewModel.onIntent(QueueIntent.Play(it)) },
        onRemove = { viewModel.onIntent(QueueIntent.Remove(it)) },
    )
}

@Composable
fun QueueContent(
    current: Episode?,
    isPlaying: Boolean,
    upNext: List<Episode>,
    onPlay: (Episode) -> Unit,
    onRemove: (Episode) -> Unit,
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(Res.string.queue)) } }
            current?.let { episode ->
                item {
                    Button(
                        onClick = { onPlay(episode) },
                        icon = {
                            if (isPlaying) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                            } else {
                                Thumbnail(episode.imageUrl)
                            }
                        },
                        label = { Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (upNext.isEmpty()) {
                item { Text(stringResource(Res.string.queue_empty_title)) }
            }
            items(upNext, key = { it.id }) { episode ->
                Button(
                    onClick = { onPlay(episode) },
                    onLongClick = { onRemove(episode) },
                    icon = { Thumbnail(episode.imageUrl) },
                    label = { Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
