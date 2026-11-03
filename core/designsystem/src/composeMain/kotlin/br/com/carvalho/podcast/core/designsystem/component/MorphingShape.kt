package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import br.com.carvalho.podcast.core.designsystem.Motion

private const val SQUARE_CORNER_PERCENT = 30
private const val ROUND_CORNER_PERCENT = 50

/**
 * A square with large corners that turns round when [round] is true, the shape change of Material 3 Expressive
 * (ADR 0001, "Layout das telas"): the play button is round while paused, a button rounds while pressed.
 */
@Composable
fun morphingShape(round: Boolean): Shape {
    val percent by animateIntAsState(
        targetValue = if (round) ROUND_CORNER_PERCENT else SQUARE_CORNER_PERCENT,
        animationSpec = tween(Motion.MEDIUM, easing = Motion.Standard),
    )
    return RoundedCornerShape(percent)
}
