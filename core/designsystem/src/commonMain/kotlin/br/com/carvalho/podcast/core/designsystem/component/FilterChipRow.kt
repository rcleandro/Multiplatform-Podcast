package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.LayoutDirection
import br.com.carvalho.podcast.core.designsystem.Spacing
import kotlin.math.roundToInt

/** One filter option; [count] shows next to the label when not null (for example, podcasts per category). */
data class FilterOption(val label: String, val count: Int? = null)

/**
 * Single-choice chips in a scrollable row: categories in the library, episode filters in the podcast.
 * [contentPadding] scrolls with the chips, so the row runs to the edges and a chip that does not fit shows cut there.
 */
@Composable
fun FilterChipRow(
    options: List<FilterOption>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val scroll = rememberScrollState()
    // Where each chip starts and ends in the row, to scroll the selected one into sight.
    val spans = remember { mutableStateMapOf<Int, IntRange>() }
    // Only this row scrolls: bringIntoView would also move the list the row sits in, hiding what is above it.
    LaunchedEffect(selectedIndex) {
        withFrameNanos { } // after the chips are placed
        val span = spans[selectedIndex] ?: return@LaunchedEffect
        val visible = scroll.value..(scroll.value + scroll.viewportSize)
        when {
            span.first < visible.first -> scroll.animateScrollTo(span.first)
            span.last > visible.last -> scroll.animateScrollTo(span.last - scroll.viewportSize)
        }
    }
    Row(
        modifier = modifier.fadingEdges(scroll).horizontalScroll(scroll).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        options.forEachIndexed { index, option ->
            FilterChip(
                selected = index == selectedIndex,
                modifier = Modifier.onPlaced { placed ->
                    val start = placed.positionInParent().x.roundToInt()
                    spans[index] = start..(start + placed.size.width)
                },
                onClick = { onSelected(index) },
                label = { Text(option.count?.let { "${option.label}  $it" } ?: option.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

/**
 * Fades the row out at each end where more chips wait, so a row that ends flush with the screen still shows it
 * scrolls (found on the Razr with six episode filters, 18.12). Fades the chips themselves, whatever is behind them.
 */
private fun Modifier.fadingEdges(scroll: ScrollState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = (Spacing.xxl.toPx() / size.width).coerceIn(0f, 1f / 2)
        val rtl = layoutDirection == LayoutDirection.Rtl
        // Scrolling forward goes right in left-to-right layouts and left in right-to-left ones.
        val left = if (rtl) scroll.canScrollForward else scroll.canScrollBackward
        val right = if (rtl) scroll.canScrollBackward else scroll.canScrollForward
        if (left || right) {
            drawRect(
                Brush.horizontalGradient(
                    0f to if (left) Color.Transparent else Color.Black,
                    fade to Color.Black,
                    1 - fade to Color.Black,
                    1f to if (right) Color.Transparent else Color.Black,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
