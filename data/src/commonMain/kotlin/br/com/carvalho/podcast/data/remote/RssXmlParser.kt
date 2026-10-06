package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.data.remote.model.RssEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.core.KtXmlReader

private const val TAG = "RssXmlParser"
private const val ITUNES_NS = "http://www.itunes.com/dtds/podcast-1.0.dtd"
private const val CONTENT_NS = "http://purl.org/rss/1.0/modules/content/"
private const val ATOM_NS = "http://www.w3.org/2005/Atom"
private const val TITLE_FALLBACK_LENGTH = 80
private val EXPLICIT_VALUES = setOf("yes", "true", "explicit")

/**
 * Reads RSS 2.0 with the iTunes tags (ADR 0004). The XML becomes a small tree first: feeds are read whole anyway,
 * and looking fields up by name is simpler than a streaming state machine.
 */
object RssXmlParser {
    fun parse(xml: String): RssFeed {
        AppLogger.d(TAG, "Starting XML parse (length: ${xml.length})")
        val channel = readTree(xml).find("channel") ?: Node("channel")
        val channelImage = channel.child("itunes:image")?.attributes?.get("href") ?: channel.child("image")?.text("url")
        val episodes = channel.children("item").mapNotNull { it.toEpisode(channelImage) }
        AppLogger.d(TAG, "Finished XML parse. Total episodes: ${episodes.size}")

        return RssFeed(
            title = channel.text("title").orEmpty(),
            description = channel.text("description") ?: channel.text("itunes:summary").orEmpty(),
            imageUrl = channelImage,
            author = channel.text("itunes:author"),
            language = channel.text("language"),
            categories = channel.categories(),
            link = channel.text("link"),
            ttl = channel.text("ttl")?.toIntOrNull(),
            episodes = episodes,
            selfUrl = channel.children("atom:link").firstOrNull { it.attributes["rel"] == "self" }
                ?.attributes?.get("href"),
            newFeedUrl = channel.text("itunes:new-feed-url"),
        )
    }

    // An item without audio (a blog post in the same feed) is not an episode.
    private fun Node.toEpisode(defaultImage: String?): RssEpisode? {
        val enclosure = child("enclosure")?.attributes
        val enclosureUrl = enclosure?.get("url")?.trim().orEmpty().ifEmpty { return null }
        val description = text("description") ?: text("content:encoded") ?: text("itunes:summary")
        // Without a title, fall back to the feed's own data instead of a fixed text.
        val title = text("title")
            ?: description?.take(TITLE_FALLBACK_LENGTH)
            ?: enclosureUrl.substringAfterLast('/').substringBefore('?')
        return RssEpisode(
            guid = text("guid"),
            title = title,
            description = description,
            enclosureUrl = enclosureUrl,
            enclosureType = enclosure?.get("type"),
            duration = text("itunes:duration"),
            publishDate = text("pubDate").orEmpty(),
            imageUrl = child("itunes:image")?.attributes?.get("href") ?: defaultImage,
            explicit = text("itunes:explicit")?.lowercase() in EXPLICIT_VALUES,
            season = text("itunes:season")?.toIntOrNull(),
            episode = text("itunes:episode")?.toIntOrNull(),
        )
    }

    // Apple nests subcategories (`Technology > Software How-To`); both levels count.
    private fun Node.categories(): List<String> = children("itunes:category").flatMap { category ->
        listOfNotNull(category.attributes["text"]) + category.categories()
    }.distinct()

    private fun readTree(xml: String): Node {
        // relaxed: real feeds have undeclared prefixes and stray characters that a strict reader rejects.
        val reader = KtXmlReader(xml, relaxed = true)
        val root = Node("")
        val stack = ArrayDeque(listOf(root))
        while (reader.hasNext()) {
            when (reader.next()) {
                EventType.START_ELEMENT -> {
                    val attributes = (0 until reader.attributeCount).associate {
                        reader.getAttributeLocalName(it) to reader.getAttributeValue(it)
                    }
                    val node = Node(reader.qualifiedName(), attributes)
                    stack.last().children += node
                    stack.addLast(node)
                }
                EventType.END_ELEMENT -> stack.removeLast()
                EventType.TEXT, EventType.CDSECT, EventType.IGNORABLE_WHITESPACE ->
                    stack.last().text.append(reader.text)
                // Known XML entities come with their text; HTML ones (&nbsp;) are undeclared and stay as written.
                EventType.ENTITY_REF ->
                    stack.last().text.append(if (reader.isKnownEntity) reader.text else "&${reader.localName};")
                else -> Unit
            }
        }
        return root
    }

    // The prefix comes from the namespace: a feed may bind iTunes to another prefix, or write Atom's link unprefixed.
    private fun KtXmlReader.qualifiedName(): String = when (namespaceURI) {
        ITUNES_NS -> "itunes:$localName"
        CONTENT_NS -> "content:$localName"
        ATOM_NS -> "atom:$localName"
        else -> if (prefix.isEmpty()) localName else "$prefix:$localName"
    }

    private class Node(val name: String, val attributes: Map<String, String> = emptyMap()) {
        val children = mutableListOf<Node>()
        val text = StringBuilder()

        fun child(name: String) = children.firstOrNull { it.name == name }
        fun children(name: String) = children.filter { it.name == name }
        fun text(name: String) = child(name)?.text?.trim()?.toString()?.ifEmpty { null }
        fun find(name: String): Node? = child(name) ?: children.firstNotNullOfOrNull { it.find(name) }
    }
}
