package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import br.com.carvalho.podcast.core.designsystem.Motion
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.generated.resources.Res
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_loading
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_pause
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_play
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_resume_progress
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

enum class PlayButtonStyle {
    /** Amber disc: the main control in the player and the mini player. */
    Filled,

    /** Soft disc used in episode rows; with [PlayPauseButton] `progress` it becomes a progress ring. */
    Tonal,
}

/**
 * Play/pause with a loading state. In [PlayButtonStyle.Tonal], a [progress] above zero draws a ring with how
 * much of the episode was already played, and the label says so.
 */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    style: PlayButtonStyle = PlayButtonStyle.Filled,
    size: Dp = Sizes.touchTarget,
    progress: Float = 0f,
    shape: Shape = CircleShape,
) {
    val showRing = style == PlayButtonStyle.Tonal && progress > 0f && !isPlaying
    val label = when {
        isLoading -> stringResource(Res.string.ds_loading)
        isPlaying -> stringResource(Res.string.ds_pause)
        showRing -> stringResource(Res.string.ds_resume_progress, (progress * PERCENT).roundToInt())
        else -> stringResource(Res.string.ds_play)
    }
    val (container, content) = when {
        showRing -> Color.Transparent to MaterialTheme.colorScheme.primary
        style == PlayButtonStyle.Filled -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = shape,
        color = container,
        contentColor = content,
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (showRing) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(size),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeWidth = Sizes.progressStroke,
                )
            }
            Crossfade(
                targetState = isLoading to isPlaying,
                animationSpec = tween(Motion.SHORT, easing = Motion.Standard),
            ) { (loading, playing) ->
                when {
                    loading -> CircularProgressIndicator(
                        modifier = Modifier.size(size * ICON_FRACTION),
                        color = content,
                        strokeWidth = Sizes.progressStroke,
                    )
                    else -> Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(size * ICON_FRACTION),
                    )
                }
            }
        }
    }
}

private const val PERCENT = 100
private const val ICON_FRACTION = 0.5f
