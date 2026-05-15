package br.com.carvalho.podcast.core.util

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.File

actual object FileUtils {
    actual val fileSystem: FileSystem = FileSystem.SYSTEM

    actual val baseDir: Path by lazy {
        val userHome = System.getProperty("user.home")
        val podcastDir = File(userHome, ".podcast")
        if (!podcastDir.exists()) {
            podcastDir.mkdirs()
        }
        podcastDir.absolutePath.toPath()
    }
}
