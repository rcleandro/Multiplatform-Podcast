package br.com.carvalho.podcast.core.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSNumber

/** Declared in PrivacyInfo.xcprivacy (disk space, reason 85F4.1: shown to the user in Settings). */
@OptIn(ExperimentalForeignApi::class)
actual fun diskSpace(path: String): DiskSpace? {
    val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null)
    val total = (attributes?.get(NSFileSystemSize) as? NSNumber)?.longLongValue
    val free = (attributes?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue
    return if (total != null && free != null) DiskSpace(totalBytes = total, freeBytes = free) else null
}
