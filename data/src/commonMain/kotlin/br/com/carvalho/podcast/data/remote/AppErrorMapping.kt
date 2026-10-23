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
    // Ktor's browser engine, when fetch() fails: a blocked (CORS) answer or no connection, the browser does not say.
    this is Error && message == BROWSER_FETCH_FAILED -> AppError.BrowserBlocked
    else -> AppError.Unknown(this)
}

/** What Ktor's browser engine (ktor-client-js 3.6) throws when the browser's fetch() fails. */
internal const val BROWSER_FETCH_FAILED = "Fail to fetch"

/** Like [runCatching], but rethrows cancellation and reports failures as [AppError]. */
@Suppress("TooGenericExceptionCaught") // the boundary where any failure becomes an AppError
internal inline fun <T> catchingAppError(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e.toAppError())
} catch (e: Error) {
    // Ktor's browser engine reports a failed fetch() as an Error, not an Exception; any other Error is a real crash.
    if (e.message != BROWSER_FETCH_FAILED) throw e
    Result.failure(AppError.BrowserBlocked)
}
