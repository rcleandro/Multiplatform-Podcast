package br.com.carvalho.podcast.core.util

import okio.FileSystem
import okio.Path

/** Where the app keeps its files (downloads); each platform module provides it. */
data class AppDirectories(val fileSystem: FileSystem, val baseDir: Path) {
    val downloadsDir: Path get() = baseDir / "downloads"

    /** The audio file of a downloaded episode; the database migration that renames ids relies on it too. */
    fun downloadPath(episodeId: String): Path = downloadsDir / "$episodeId.mp3"
}
