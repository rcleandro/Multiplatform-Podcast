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

    /**
     * The feed is bigger than the app reads (22.3): a real one never gets there, a hostile one would fill the
     * memory.
     */
    data object FeedTooLarge : AppError()
    data object AlreadyExists : AppError()
    data object NotFound : AppError()
    data object StorageFull : AppError()

    /**
     * The browser would not hand the feed to the Web app: most podcast hosts send no CORS header (20.4, ADR 0013).
     * The browser reports a lost connection the same way, so the message covers both.
     */
    data object BrowserBlocked : AppError()

    /** The downloads already take the space the user allowed them in Settings (18.5). */
    data object StorageLimit : AppError()
    class Unknown(cause: Throwable?) : AppError(cause)
}
