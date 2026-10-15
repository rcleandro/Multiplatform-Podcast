package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.extensions.toTime
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.skip_backward
import br.com.carvalho.podcast.core.ui.generated.resources.skip_forward
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.feature.player.presentation.PlayerIntent
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The episode playing: its title, where it is, and the jumps and play button. The side buttons set the volume. */
@Composable
fun NowPlayingScreen(viewModel: PlayerViewModel = koinViewModel()) {
    val state by viewModel.playerState.collectAsState()
    NowPlayingContent(state, viewModel::onIntent)
}

@Composable
fun NowPlayingContent(state: PlayerState, onIntent: (PlayerIntent) -> Unit) {
    val playSize = 56.dp
    val skips = state.skipIntervals
    ScreenScaffold {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.xl),
        ) {
            Text(
                state.currentEpisode?.title.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(state.position.toTime(), style = MaterialTheme.typography.labelSmall)
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
        }
    }
}
