package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.carvalho.podcast.core.designsystem.icons.FastForward
import br.com.carvalho.podcast.core.designsystem.icons.FastRewind
import br.com.carvalho.podcast.core.designsystem.icons.Forward10
import br.com.carvalho.podcast.core.designsystem.icons.Forward30
import br.com.carvalho.podcast.core.designsystem.icons.Forward5
import br.com.carvalho.podcast.core.designsystem.icons.Replay10
import br.com.carvalho.podcast.core.designsystem.icons.Replay30
import br.com.carvalho.podcast.core.designsystem.icons.Replay5

/** The icon with the jump's number when Material has one (5, 10, 30 s); a plain arrow otherwise. */
fun skipForwardIcon(seconds: Int): ImageVector = when (seconds) {
    SHORT_JUMP -> Icons.Rounded.Forward5
    MEDIUM_JUMP -> Icons.Rounded.Forward10
    LONG_JUMP -> Icons.Rounded.Forward30
    else -> Icons.Rounded.FastForward
}

fun skipBackwardIcon(seconds: Int): ImageVector = when (seconds) {
    SHORT_JUMP -> Icons.Rounded.Replay5
    MEDIUM_JUMP -> Icons.Rounded.Replay10
    LONG_JUMP -> Icons.Rounded.Replay30
    else -> Icons.Rounded.FastRewind
}

private const val SHORT_JUMP = 5
private const val MEDIUM_JUMP = 10
private const val LONG_JUMP = 30
