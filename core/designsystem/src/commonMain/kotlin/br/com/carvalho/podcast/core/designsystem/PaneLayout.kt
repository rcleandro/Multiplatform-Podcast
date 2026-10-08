package br.com.carvalho.podcast.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass

/**
 * How much width the window gives (21.1), decided by its size and never by the device model: a phone, a phone
 * on its side or a narrow window is [COMPACT] (under 600 dp), a small tablet or a half-screen window is [MEDIUM]
 * (600–840 dp) and anything wider is [EXPANDED].
 */
enum class PaneLayout {
    COMPACT,
    MEDIUM,
    EXPANDED;

    companion object {
        fun from(sizeClass: WindowSizeClass): PaneLayout = when {
            sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> EXPANDED
            sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> MEDIUM
            else -> COMPACT
        }
    }
}

/** The [PaneLayout] of the current window. */
@Composable
fun currentPaneLayout(): PaneLayout = PaneLayout.from(currentWindowAdaptiveInfo().windowSizeClass)

/** A phone on its side or a low window (under 480 dp of height, 21.2): screens trade height for width. */
fun isShortWindow(sizeClass: WindowSizeClass): Boolean =
    !sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

/** Whether the current window is short; see [isShortWindow]. */
@Composable
fun currentWindowIsShort(): Boolean = isShortWindow(currentWindowAdaptiveInfo().windowSizeClass)

/**
 * Takes the whole width but keeps the content within [Sizes.readableWidth], centered: on a phone nothing changes,
 * on a large window a list item or a block of text stops stretching from edge to edge. Applied per item rather
 * than to a whole list, so the list still scrolls from its edges and backgrounds still fill the window.
 */
fun Modifier.readableWidth(): Modifier = fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = Sizes.readableWidth)
    .fillMaxWidth()
