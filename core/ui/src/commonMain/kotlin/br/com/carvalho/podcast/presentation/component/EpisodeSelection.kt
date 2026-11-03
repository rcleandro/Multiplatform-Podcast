package br.com.carvalho.podcast.presentation.component

import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.bulk_downloading
import br.com.carvalho.podcast.core.ui.generated.resources.bulk_played
import br.com.carvalho.podcast.core.ui.generated.resources.bulk_queued
import br.com.carvalho.podcast.core.ui.generated.resources.bulk_unplayed
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.usecase.EpisodeAction
import br.com.carvalho.podcast.presentation.UiMessage

/** Marking [episode] and every older one of its podcast as [played] or not, waiting for the user to confirm. */
data class OlderMark(val episode: Episode, val played: Boolean)

/** What the snackbar says after acting on [count] selected episodes. */
fun EpisodeAction.message(count: Int): UiMessage = UiMessage(
    when (this) {
        EpisodeAction.ADD_TO_QUEUE -> Res.string.bulk_queued
        EpisodeAction.DOWNLOAD -> Res.string.bulk_downloading
        EpisodeAction.MARK_PLAYED -> Res.string.bulk_played
        EpisodeAction.MARK_UNPLAYED -> Res.string.bulk_unplayed
    },
    listOf(count),
)

/** The selection with [episode] added, or taken out when it was already there; the order is the order chosen. */
fun List<Episode>.toggled(episode: Episode): List<Episode> =
    if (any { it.id == episode.id }) filterNot { it.id == episode.id } else this + episode
