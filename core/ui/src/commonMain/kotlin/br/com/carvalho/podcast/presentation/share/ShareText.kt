package br.com.carvalho.podcast.presentation.share

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast

private const val MILLIS_PER_SECOND = 1000

/** A podcast as shared (18.4): its name and its site, or its feed when it has no site. */
fun podcastShareText(podcast: Podcast): String = "${podcast.title}\n${podcast.siteUrl ?: podcast.feedUrl}"

/**
 * An episode as shared (18.4): its title and podcast, [startsAt] (the "from 12:34" line when it comes from the player)
 * and where to hear it: its page on the podcast's site, else its audio, which browsers start at [positionMs] (`#t=`).
 */
fun episodeShareText(episode: Episode, startsAt: String? = null, positionMs: Long? = null): String {
    val seconds = positionMs?.div(MILLIS_PER_SECOND)?.takeIf { it > 0 }
    val link = episode.link ?: episode.audioUrl.let { if (seconds != null) "$it#t=$seconds" else it }
    val title = listOfNotNull(episode.title, episode.podcastTitle).joinToString(" · ")
    return listOfNotNull(title, startsAt, link).joinToString("\n")
}
