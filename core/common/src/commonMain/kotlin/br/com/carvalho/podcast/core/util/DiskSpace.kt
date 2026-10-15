package br.com.carvalho.podcast.core.util

/** Size and free space of the disk that holds a folder, in bytes. */
data class DiskSpace(val totalBytes: Long, val freeBytes: Long)

/** The disk that holds [path]; null where there is no disk to speak of (the Web) or the system does not say. */
expect fun diskSpace(path: String): DiskSpace?
