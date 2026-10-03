package br.com.carvalho.podcast.core.util

import okio.FileSystem
import okio.Path

/** Where the app keeps its files (downloads); each platform module provides it. */
data class AppDirectories(val fileSystem: FileSystem, val baseDir: Path) {
    val downloadsDir: Path get() = baseDir / "downloads"

    /** A file in the downloads folder; the episode's file name is stored in the database. */
    fun downloadPath(fileName: String): Path = downloadsDir / fileName
}
