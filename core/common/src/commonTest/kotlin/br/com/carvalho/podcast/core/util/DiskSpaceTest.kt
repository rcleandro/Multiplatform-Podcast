package br.com.carvalho.podcast.core.util

import kotlin.test.Test
import kotlin.test.assertTrue

class DiskSpaceTest {
    /** The root exists everywhere there is a disk; the Web has none and says so. */
    @Test
    fun theDiskHasRoomForWhatIsFree() {
        val disk = diskSpace("/")

        assertTrue(disk == null || disk.totalBytes >= disk.freeBytes && disk.freeBytes >= 0, "$disk")
        assertTrue(disk != null || !supportsDownloads, "A platform with downloads should know its disk")
    }
}
