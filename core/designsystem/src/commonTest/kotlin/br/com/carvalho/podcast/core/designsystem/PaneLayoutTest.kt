package br.com.carvalho.podcast.core.designsystem

import androidx.window.core.layout.WindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaneLayoutTest {

    private fun layoutFor(widthDp: Int) = PaneLayout.from(WindowSizeClass(minWidthDp = widthDp, minHeightDp = 800))

    @Test
    fun aPhoneOrANarrowWindowIsCompact() {
        assertEquals(PaneLayout.COMPACT, layoutFor(360))
        assertEquals(PaneLayout.COMPACT, layoutFor(599))
    }

    @Test
    fun fromSixHundredItIsMedium() {
        assertEquals(PaneLayout.MEDIUM, layoutFor(600))
        assertEquals(PaneLayout.MEDIUM, layoutFor(839))
    }

    @Test
    fun fromEightHundredFortyItIsExpanded() {
        assertEquals(PaneLayout.EXPANDED, layoutFor(840))
        assertEquals(PaneLayout.EXPANDED, layoutFor(1600))
    }

    @Test
    fun underFourHundredEightyOfHeightTheWindowIsShort() {
        assertTrue(isShortWindow(WindowSizeClass(minWidthDp = 800, minHeightDp = 400)))
        assertFalse(isShortWindow(WindowSizeClass(minWidthDp = 400, minHeightDp = 480)))
    }
}
