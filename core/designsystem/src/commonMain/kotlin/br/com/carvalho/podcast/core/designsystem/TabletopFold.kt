package br.com.carvalho.podcast.core.designsystem

import androidx.compose.runtime.compositionLocalOf

/**
 * A foldable half open with its fold across the screen, like a laptop on a table (21.3): [top] and [bottom] are the
 * fold's edges in window pixels. Screens put what is looked at above it and what is touched below it.
 */
data class TabletopFold(val top: Int, val bottom: Int)

/** The fold of the window while in tabletop posture; null otherwise and on platforms without folds. */
val LocalTabletopFold = compositionLocalOf<TabletopFold?> { null }

/** The [TabletopFold] for a fold of the platform: only a half open fold across the screen is one. */
fun tabletopFold(isHalfOpened: Boolean, isAcross: Boolean, top: Int, bottom: Int): TabletopFold? =
    if (isHalfOpened && isAcross) TabletopFold(top, bottom) else null
