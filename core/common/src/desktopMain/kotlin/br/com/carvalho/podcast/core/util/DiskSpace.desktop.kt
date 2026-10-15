package br.com.carvalho.podcast.core.util

import java.io.File

actual fun diskSpace(path: String): DiskSpace? =
    File(path).takeIf { it.exists() }?.let { DiskSpace(totalBytes = it.totalSpace, freeBytes = it.usableSpace) }
