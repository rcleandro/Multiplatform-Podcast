package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import br.com.carvalho.podcast.core.designsystem.Spacing
import kotlin.math.roundToInt

/** One filter option; [count] shows next to the label when not null (for example, podcasts per category). */
data class FilterOption(val label: String, val count: Int? = null)

/** Single-choice chips in a scrollable row: categories in the library, episode filters in the podcast. */
@Composable
fun FilterChipRow(
    options: List<FilterOption>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier.horizontalScroll(scroll),
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
