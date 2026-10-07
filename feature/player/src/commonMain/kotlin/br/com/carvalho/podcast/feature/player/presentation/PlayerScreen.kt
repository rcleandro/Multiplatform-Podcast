package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.viewmodel.koinViewModel

private enum class PlayerDialog { Speed, SleepTimer, Queue }

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    artworkModifier: Modifier = Modifier,
) {
    val playerState by viewModel.playerState.collectAsState()
    var dialog by remember { mutableStateOf<PlayerDialog?>(null) }

    PlayerContent(
        state = playerState,
        artworkModifier = artworkModifier,
        actions = PlayerActions(
            onMinimize = onBackClick,
            onPlayPause = { viewModel.onIntent(PlayerIntent.PlayPause) },
            onSeek = { viewModel.onIntent(PlayerIntent.SeekTo(it)) },
            onSkipBackward = { viewModel.onIntent(PlayerIntent.SkipBackward) },
            onSkipForward = { viewModel.onIntent(PlayerIntent.SkipForward) },
            onPrevious = { viewModel.onIntent(PlayerIntent.Previous) },
            onNext = { viewModel.onIntent(PlayerIntent.Next) },
            onSpeedClick = { dialog = PlayerDialog.Speed },
            onQueueClick = { dialog = PlayerDialog.Queue },
            onSleepTimerClick = { dialog = PlayerDialog.SleepTimer },
        ),
    )

    when (dialog) {
        PlayerDialog.Speed -> SpeedSelectorDialog(
            currentSpeed = playerState.speed,
            onSpeedSelected = {
                viewModel.onIntent(PlayerIntent.SetSpeed(it))
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        PlayerDialog.SleepTimer -> SleepTimerDialog(
            playerState = playerState,
            onTimerSelected = {
                viewModel.onIntent(PlayerIntent.SetSleepTimer(it))
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        PlayerDialog.Queue -> QueueDialog(
            queue = playerState.queue,
            currentEpisodeId = playerState.currentEpisode?.id,
            onEpisodeSelected = {
                viewModel.onIntent(PlayerIntent.Play(it))
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}
