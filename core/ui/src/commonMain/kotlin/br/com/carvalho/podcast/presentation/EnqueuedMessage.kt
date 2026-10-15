package br.com.carvalho.podcast.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.already_playing
import br.com.carvalho.podcast.core.ui.generated.resources.queued_last
import br.com.carvalho.podcast.core.ui.generated.resources.queued_next
import br.com.carvalho.podcast.domain.usecase.Enqueued

/** What to say after "Play next" or "Add to queue" (18.6); nothing when the episode simply started playing. */
fun Enqueued.message(): UiMessage? = when (this) {
    Enqueued.NEXT -> UiMessage(Res.string.queued_next)
    Enqueued.LAST -> UiMessage(Res.string.queued_last)
    Enqueued.ALREADY_PLAYING -> UiMessage(Res.string.already_playing)
    Enqueued.PLAYING -> null
}
