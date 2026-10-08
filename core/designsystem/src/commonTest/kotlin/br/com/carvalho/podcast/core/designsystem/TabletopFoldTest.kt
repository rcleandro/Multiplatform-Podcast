package br.com.carvalho.podcast.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TabletopFoldTest {

    @Test
    fun aHalfOpenFoldAcrossTheScreenIsTabletop() {
        assertEquals(TabletopFold(top = 1300, bottom = 1340), tabletopFold(true, isAcross = true, top = 1300, bottom = 1340))
    }

    @Test
    fun aFlatOrAnUprightFoldIsNot() {
        assertNull(tabletopFold(isHalfOpened = false, isAcross = true, top = 1300, bottom = 1340))
        assertNull(tabletopFold(isHalfOpened = true, isAcross = false, top = 0, bottom = 2640))
    }
}
