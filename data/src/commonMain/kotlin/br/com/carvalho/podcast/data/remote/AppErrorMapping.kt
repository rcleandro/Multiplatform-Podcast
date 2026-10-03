package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException

/**
 * Turns what Ktor, Okio or the parser throw into an [AppError]. Disk full is only reported through the message
 * (ENOSPC), so it is checked first: on the JVM, file and network failures share `java.io.IOException`.
 */
internal fun Throwable.toAppError(): AppError = when {
    this is AppError -> this
    // ponytail: text match on the OS message; replace if a platform exposes a typed "no space" error
    message.orEmpty().let { "ENOSPC" in it || "No space left" in it } -> AppError.StorageFull
    this is kotlinx.io.IOException || this is okio.IOException || this is UnresolvedAddressException ->
        AppError.NoConnection
    else -> AppError.Unknown(this)
}

/** Like [runCatching], but rethrows cancellation and reports failures as [AppError]. */
@Suppress("TooGenericExceptionCaught") // the boundary where any failure becomes an AppError
internal inline fun <T> catchingAppError(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e.toAppError())
}
