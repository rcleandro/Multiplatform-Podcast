package br.com.carvalho.podcast.core.designsystem

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Height the mini player covers at the bottom of the screen right now (0 when it is hidden). The navigation draws the
 * mini player over the screens, so whatever sits at their bottom edge (messages, the add button) moves up by this.
 */
val LocalMiniPlayerInset = compositionLocalOf { 0.dp }
