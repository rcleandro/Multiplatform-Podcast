package br.com.carvalho.podcast.core.di

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import okio.Path.Companion.toPath
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalForeignApi::class)
class BackupExclusionTest {
    // Downloads come back from the feed: neither iCloud nor a computer backup takes them (22.7).
    @Test
    fun theDownloadsFolderIsMadeAndLeftOutOfBackups() = memScoped {
        val folder = (NSTemporaryDirectory() + NSUUID().UUIDString).toPath() / "downloads"

        excludeFromBackup(folder)

        val excluded = alloc<ObjCObjectVar<Any?>>()
        NSURL.fileURLWithPath(folder.toString()).getResourceValue(excluded.ptr, NSURLIsExcludedFromBackupKey, null)
        assertEquals(true, (excluded.value as? NSNumber)?.boolValue)
    }
}
