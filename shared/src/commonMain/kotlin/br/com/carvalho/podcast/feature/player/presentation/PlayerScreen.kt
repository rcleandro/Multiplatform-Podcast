package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.runtime.Composable
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
    onBackClick: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    var dialog by remember { mutableStateOf<PlayerDialog?>(null) }

    PlayerContent(
        state = playerState,
        actions = PlayerActions(
            onMinimize = onBackClick,
            onPlayPause = { if (playerState.isPlaying) viewModel.pause() else viewModel.resume() },
            onSeek = viewModel::seekTo,
            onSkipBackward = viewModel::skipBackward,
            onSkipForward = viewModel::skipForward,
            onPrevious = viewModel::playPrevious,
            onNext = viewModel::playNext,
            onSpeedClick = { dialog = PlayerDialog.Speed },
            onQueueClick = { dialog = PlayerDialog.Queue },
            onSleepTimerClick = { dialog = PlayerDialog.SleepTimer },
        ),
    )

    when (dialog) {
        PlayerDialog.Speed -> SpeedSelectorDialog(
            currentSpeed = playerState.speed,
            onSpeedSelected = {
                viewModel.setSpeed(it)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        PlayerDialog.SleepTimer -> SleepTimerDialog(
            playerState = playerState,
            onTimerSelected = {
                viewModel.setSleepTimer(it)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        PlayerDialog.Queue -> QueueDialog(
            queue = playerState.queue,
            currentEpisodeId = playerState.currentEpisode?.id,
            onEpisodeSelected = {
                viewModel.play(it)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}
