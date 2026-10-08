package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.ui.geometry.Rect
import androidx.window.core.layout.WindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class PaneDirectiveTest {

    @Test
    fun aFoldOpenFlatKeepsThePanesOffItsCrease() {
        val crease = Rect(left = 840f, top = 0f, right = 841f, bottom = 1840f)
        val unfolded = WindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass(minWidthDp = 840, minHeightDp = 700),
            windowPosture = Posture(
                hingeList = listOf(
                    HingeInfo(crease, isFlat = true, isVertical = true, isSeparating = false, isOccluding = false),
                ),
            ),
        )

        assertEquals(listOf(crease), paneDirective(unfolded).excludedBounds)
    }
}
