package br.com.carvalho.podcast.presentation.share

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.usecase.mayCarrySecret

private const val MILLIS_PER_SECOND = 1000

/**
 * A podcast as shared (18.4): its name and its site, or its feed when it has no site. A private feed's address is
 * never shared (22.6): whoever got the message would hear the paid feed, so with no site only the name goes.
 */
fun podcastShareText(podcast: Podcast): String {
    val link = podcast.siteUrl ?: podcast.feedUrl.takeUnless { mayCarrySecret(it) }
    return listOfNotNull(podcast.title, link).joinToString("\n")
}

/**
 * An episode as shared (18.4): its title and podcast, [startsAt] (the "from 12:34" line when it comes from the player)
 * and where to hear it: its page on the podcast's site, else its audio, which browsers start at [positionMs] (`#t=`).
 */
fun episodeShareText(episode: Episode, startsAt: String? = null, positionMs: Long? = null): String {
    val seconds = positionMs?.div(MILLIS_PER_SECOND)?.takeIf { it > 0 }
    // A private feed's audio carries its token too (22.6); the podcast's id is its feed address (RssMapper).
    val audio = episode.audioUrl.takeUnless { mayCarrySecret(episode.podcastId) || mayCarrySecret(it) }
    val link = episode.link ?: audio?.let { if (seconds != null) "$it#t=$seconds" else it }
    val title = listOfNotNull(episode.title, episode.podcastTitle).joinToString(" · ")
    return listOfNotNull(title, startsAt, link).joinToString("\n")
}
