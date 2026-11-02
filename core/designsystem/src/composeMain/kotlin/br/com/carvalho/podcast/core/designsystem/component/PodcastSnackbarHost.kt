package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.LocalMiniPlayerInset

/**
 * Every screen's snackbar host: above the mini player when it is showing. A screen with a floating button passes
 * [clearMiniPlayer] false: the Scaffold already puts the message above the button, which clears the mini player.
 */
@Composable
fun PodcastSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier, clearMiniPlayer: Boolean = true) {
    val inset = if (clearMiniPlayer) LocalMiniPlayerInset.current else 0.dp
    SnackbarHost(state, modifier = modifier.padding(bottom = inset))
}
