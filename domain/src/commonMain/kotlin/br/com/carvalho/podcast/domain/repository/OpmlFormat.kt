package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Podcast

/** OPML, the file podcast apps trade subscriptions in (18.13). */
interface OpmlFormat {
    /** The feed addresses an OPML file lists, in its order, each once; a file that is not OPML lists none. */
    fun read(opml: String): List<String>

    fun write(podcasts: List<Podcast>): String
}
