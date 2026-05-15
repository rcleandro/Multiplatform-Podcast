package br.com.carvalho.podcast.core.util

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual object FileUtils {
    actual val fileSystem: FileSystem = FileSystem.SYSTEM

    actual val baseDir: Path by lazy {
        val manager = NSFileManager.defaultManager
        val url = manager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as platform.Foundation.NSURL
        url.path!!.toPath()
    }
}
