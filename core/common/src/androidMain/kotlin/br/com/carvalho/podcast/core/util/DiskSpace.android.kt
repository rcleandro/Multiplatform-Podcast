package br.com.carvalho.podcast.core.util

import android.os.StatFs

actual fun diskSpace(path: String): DiskSpace? = runCatching {
    StatFs(path).let { DiskSpace(totalBytes = it.totalBytes, freeBytes = it.availableBytes) }
}.getOrNull()
