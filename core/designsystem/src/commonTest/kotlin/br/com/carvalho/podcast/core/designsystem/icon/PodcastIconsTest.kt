package br.com.carvalho.podcast.core.designsystem.icon

import androidx.compose.ui.graphics.vector.VectorPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PodcastIconsTest {
    @Test
    fun markUnplayedIsACheckStruckThroughOnMaterialsGrid() {
        val icon = PodcastIcons.MarkUnplayed
        assertEquals(24f, icon.viewportWidth)
        val (check, strike) = icon.root.map { it as VectorPath }
        // The check is filled; the strike is a stroke, so both take the icon's tint.
        assertNotNull(check.fill)
        assertNull(check.stroke)
        assertNotNull(strike.stroke)
        assertEquals(2f, strike.strokeLineWidth)
    }
}
