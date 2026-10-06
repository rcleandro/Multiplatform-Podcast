package br.com.carvalho.podcast.core

/**
 * Every failure the app shows to the user. Data turns exceptions into one of these; the UI turns them into text.
 * It is an [Exception] so it travels inside [Result.failure].
 */
sealed class AppError(cause: Throwable? = null) : Exception(cause) {
    data object NoConnection : AppError()
    data class Http(val status: Int) : AppError()

    /** What the user typed cannot be a feed address. */
    data object InvalidUrl : AppError()

    /** The URL answered, but not with an RSS feed (a web page, for instance). */
    data object InvalidFeed : AppError()
    data object AlreadyExists : AppError()
    data object NotFound : AppError()
    data object StorageFull : AppError()
    class Unknown(cause: Throwable?) : AppError(cause)
}
