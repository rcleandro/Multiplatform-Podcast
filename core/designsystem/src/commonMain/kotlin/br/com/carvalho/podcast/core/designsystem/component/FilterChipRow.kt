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
import androidx.compose.ui.Modifier
import br.com.carvalho.podcast.core.designsystem.Spacing

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
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        options.forEachIndexed { index, option ->
            FilterChip(
                selected = index == selectedIndex,
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
