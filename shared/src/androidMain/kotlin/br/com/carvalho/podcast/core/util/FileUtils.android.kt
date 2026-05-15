package br.com.carvalho.podcast.core.util

import android.content.Context
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual object FileUtils {
    actual val fileSystem: FileSystem = FileSystem.SYSTEM

    actual val baseDir: Path by lazy {
        val context = AppContext.context as Context
        context.filesDir.absolutePath.toPath()
    }
}
