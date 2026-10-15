package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Podcast

/** One feed address per line, so OPML imports and exports are tested without XML. */
object FakeOpmlFormat : OpmlFormat {
    override fun read(opml: String) = opml.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    override fun write(podcasts: List<Podcast>) = podcasts.joinToString("\n") { it.feedUrl }
}
