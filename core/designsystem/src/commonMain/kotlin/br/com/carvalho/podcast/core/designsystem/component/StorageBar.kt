package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.Spacing

/** What a part of the disk holds; each kind has its color. */
enum class StorageKind { APP, OTHER, FREE }

data class StorageSegment(val bytes: Long, val kind: StorageKind, val label: String)

/**
 * A disk in one bar, its [segments] side by side over [totalBytes], with an optional limit marked across it and a
 * legend below that ends with [caption]. Screen readers hear [description] instead of the pieces.
 */
@Composable
fun StorageBar(
    segments: List<StorageSegment>,
    totalBytes: Long,
    caption: String,
    description: String,
    modifier: Modifier = Modifier,
    limitBytes: Long? = null,
) {
    val barHeight = 12.dp
    val markWidth = 2.dp
    val total = totalBytes.coerceAtLeast(1)
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(barHeight)) {
            Row(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                segments.filter { it.bytes > 0 }.forEach { segment ->
                    Box(
                        modifier = Modifier
                            .weight(segment.bytes.toFloat() / total)
                            .fillMaxHeight()
                            .background(segment.kind.color()),
                    )
                }
            }
            limitBytes?.takeIf { it < total }?.let {
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * (it.toFloat() / total) - markWidth / 2)
                        .width(markWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            segments.forEach { Legend(it.kind.color(), it.label) }
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageKind.color(): Color = when (this) {
    StorageKind.APP -> MaterialTheme.colorScheme.primary
    StorageKind.OTHER -> MaterialTheme.colorScheme.outline
    StorageKind.FREE -> MaterialTheme.colorScheme.surfaceContainerHighest
}

@Composable
private fun Legend(color: Color, label: String) {
    val dotSize = 8.dp
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
