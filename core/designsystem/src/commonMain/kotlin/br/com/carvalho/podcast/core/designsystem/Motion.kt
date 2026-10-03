package br.com.carvalho.podcast.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/** Durations (ms) and curves from ADR 0001. */
object Motion {
    /** Button states, play/pause icon swap, chips. */
    const val SHORT = 150

    /** Mini player showing and hiding, artwork crossfade. */
    const val MEDIUM = 250

    /** Elements that move across the screen, such as the mini player opening into the player. */
    const val LONG = 400

    val Standard: Easing = CubicBezierEasing(a = 0.2f, b = 0f, c = 0f, d = 1f)

    /** Only for elements that change place on screen. */
    val Emphasized: Easing = CubicBezierEasing(a = 0.05f, b = 0.7f, c = 0.1f, d = 1f)
}
