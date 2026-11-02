package br.com.carvalho.podcast.presentation.format

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.size_gigabytes
import br.com.carvalho.podcast.core.ui.generated.resources.size_megabytes
import org.jetbrains.compose.resources.stringResource

/** Disk space as a value; [text] turns it into words ("850 MB", "1,2 GB"). */
sealed interface StorageSize {
    data class Megabytes(val count: Long) : StorageSize

    /** [whole] and [tenth] of a gigabyte, kept apart so the language picks the decimal separator. */
    data class Gigabytes(val whole: Long, val tenth: Long) : StorageSize
}

private const val BYTES_PER_MB = 1024L * 1024L
private const val MB_PER_GB = 1024L
private const val TENTHS = 10L

/** Megabytes round up, so a few bytes never read as "0 MB"; gigabytes round to the nearest tenth. */
fun storageSize(bytes: Long): StorageSize {
    val bytesPerGb = BYTES_PER_MB * MB_PER_GB
    if (bytes < bytesPerGb) return StorageSize.Megabytes((bytes + BYTES_PER_MB - 1) / BYTES_PER_MB)
    val tenths = (bytes * TENTHS + bytesPerGb / 2) / bytesPerGb
    return StorageSize.Gigabytes(tenths / TENTHS, tenths % TENTHS)
}

@Composable
fun StorageSize.text(): String = when (this) {
    is StorageSize.Megabytes -> stringResource(Res.string.size_megabytes, count)
    is StorageSize.Gigabytes -> stringResource(Res.string.size_gigabytes, whole, tenth)
}
