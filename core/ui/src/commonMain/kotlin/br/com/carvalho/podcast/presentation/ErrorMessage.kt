package br.com.carvalho.podcast.presentation

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_invalid_feed
import br.com.carvalho.podcast.core.ui.generated.resources.error_invalid_url
import br.com.carvalho.podcast.core.ui.generated.resources.error_no_connection
import br.com.carvalho.podcast.core.ui.generated.resources.error_podcast_exists
import br.com.carvalho.podcast.core.ui.generated.resources.error_server
import br.com.carvalho.podcast.core.ui.generated.resources.error_storage_full
import org.jetbrains.compose.resources.StringResource

/** What to tell the user about [this] failure; [fallback] names the action that failed when the cause says nothing. */
fun Throwable.toMessage(fallback: StringResource): StringResource = when (this) {
    AppError.NoConnection -> Res.string.error_no_connection
    is AppError.Http -> Res.string.error_server
    AppError.InvalidFeed -> Res.string.error_invalid_feed
    AppError.InvalidUrl -> Res.string.error_invalid_url
    AppError.AlreadyExists -> Res.string.error_podcast_exists
    AppError.StorageFull -> Res.string.error_storage_full
    else -> fallback
}
