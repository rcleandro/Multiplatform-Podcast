package br.com.carvalho.podcast.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val VIEWPORT = 24f
private const val STRIKE_WIDTH = 2f
private const val STRIKE_START = 3.5f
private const val STRIKE_END = 20.5f

// Material Rounded's "done" check, to strike through.
private const val ROUNDED_CHECK = "M9 16.17L5.53 12.7a.996.996 0 0 0-1.41 0c-.39.39-.39 1.02 0 1.41l4.18 4.18" +
    "c.39.39 1.02.39 1.41 0L20.29 7.71a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0L9 16.17z"

/** Icons Material lacks, drawn in its Rounded style so they sit among its own; tinted like any icon. */
object PodcastIcons {
    /**
     * "Mark as unplayed" for one episode: a check struck through, the single partner of Material's "remove_done"
     * (two checks struck through), which marks the older episodes.
     */
    val MarkUnplayed: ImageVector by lazy {
        ImageVector.Builder(
            name = "MarkUnplayed",
            defaultWidth = VIEWPORT.dp,
            defaultHeight = VIEWPORT.dp,
            viewportWidth = VIEWPORT,
            viewportHeight = VIEWPORT,
        )
            .addPath(pathData = addPathNodes(ROUNDED_CHECK), fill = SolidColor(Color.Black))
            .addPath(
                pathData = addPathNodes("M$STRIKE_START $STRIKE_START L$STRIKE_END $STRIKE_END"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STRIKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
            )
            .build()
    }
}
