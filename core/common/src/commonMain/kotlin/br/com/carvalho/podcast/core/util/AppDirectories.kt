package br.com.carvalho.podcast.core.util

import okio.FileSystem
import okio.Path

/** Where the app keeps its files (downloads); each platform module provides it. */
data class AppDirectories(val fileSystem: FileSystem, val baseDir: Path)
