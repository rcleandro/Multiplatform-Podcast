package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.OpmlFormat
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.core.KtXmlReader

/**
 * OPML 2.0 as podcast apps write it (18.13): an `outline` per feed with its address in `xmlUrl`, at any depth, since
 * some apps nest them in folders. Read with the same lenient reader as the feeds (ADR 0004).
 */
object XmlOpmlFormat : OpmlFormat {
    override fun read(opml: String): List<String> {
        if (declaresEntities(opml, root = "<opml")) return emptyList()
        val reader = KtXmlReader(opml, relaxed = true)
        val feeds = mutableListOf<String>()
        // ponytail: a file that is not XML ends the reading where it breaks, keeping what came before.
        runCatching {
            while (reader.hasNext()) {
                if (reader.next() == EventType.START_ELEMENT && reader.localName == "outline") {
                    val url = (0 until reader.attributeCount)
                        .firstOrNull { reader.getAttributeLocalName(it).equals("xmlUrl", ignoreCase = true) }
                        ?.let { reader.getAttributeValue(it).webUrl() }
                    if (url != null) feeds += url
                }
            }
        }
        return feeds.distinct()
    }

    override fun write(podcasts: List<Podcast>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<opml version="2.0">""")
        appendLine("""  <head><title>Podcasts</title></head>""")
        appendLine("""  <body>""")
        podcasts.forEach { podcast ->
            val title = podcast.title.escaped()
            val site = podcast.siteUrl?.let { """ htmlUrl="${it.escaped()}"""" }.orEmpty()
            appendLine(
                """    <outline type="rss" text="$title" title="$title" xmlUrl="${podcast.feedUrl.escaped()}"$site/>"""
            )
        }
        appendLine("""  </body>""")
        appendLine("""</opml>""")
    }

    private fun String.escaped(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
