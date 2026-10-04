package br.com.carvalho.podcast.presentation.format

import kotlin.test.Test
import kotlin.test.assertEquals

class StorageSizeTest {
    @Test
    fun belowAGigabyteIsWholeMegabytesRoundedUp() {
        assertEquals(StorageSize.Megabytes(0), storageSize(0))
        assertEquals(StorageSize.Megabytes(1), storageSize(1))
        assertEquals(StorageSize.Megabytes(MB_IN_GB), storageSize(GB - 1))
    }

    @Test
    fun fromAGigabyteOnIsGigabytesWithOneTenth() {
        assertEquals(StorageSize.Gigabytes(1, 0), storageSize(GB))
        assertEquals(StorageSize.Gigabytes(1, 2), storageSize(GB + GB / FIVE))
        assertEquals(StorageSize.Gigabytes(TWELVE, 0), storageSize(GB * TWELVE))
    }

    private companion object {
        const val MB_IN_GB = 1024L
        const val GB = 1024L * 1024L * 1024L
        const val FIVE = 5L
        const val TWELVE = 12L
    }
}
