package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.data.remote.model.RssEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.data.remote.model.RssTranscript
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.core.KtXmlReader

private const val TAG = "RssXmlParser"
private const val ITUNES_NS = "http://www.itunes.com/dtds/podcast-1.0.dtd"
private const val CONTENT_NS = "http://purl.org/rss/1.0/modules/content/"
private const val ATOM_NS = "http://www.w3.org/2005/Atom"

// Podcasting 2.0: the URI the spec gives, the one older feeds copied, and the spec's own page that some still use.
private val PODCAST_NS = setOf(
    "https://podcastindex.org/namespace/1.0",
    "http://podcastindex.org/namespace/1.0",
    "https://github.com/Podcastindex-org/podcast-namespace/blob/main/docs/1.0.md",
)
private const val TITLE_FALLBACK_LENGTH = 80

/** Real feeds nest a handful of levels; a deeper one is built to overflow the stack (22.3). */
private const val MAX_DEPTH = 64
private val EXPLICIT_VALUES = setOf("yes", "true", "explicit")

/** The timed transcript types, best first, as parts of their MIME types. */
private val TRANSCRIPT_TYPES = listOf("vtt", "srt", "subrip", "json")

/**
 * Reads RSS 2.0 with the iTunes tags (ADR 0004). Each item becomes a small tree, read into an episode as soon as it
 * closes and then dropped: looking fields up by name is simpler than a streaming state machine, and the tree of a
 * whole 4,000-episode feed held as much memory again as its text (23.6).
 */
object RssXmlParser {
    fun parse(xml: String): RssFeed {
        AppLogger.d(TAG, "Starting XML parse (length: ${xml.length})")
        // The reader expands entities a DOCTYPE declares, without bound: a few KB of nested ones fill the memory (the
        // billion laughs, T3 of ADR 0014). No real feed declares any, so such a feed is refused before it is read.
        if (declaresEntities(xml, root = "<channel")) throw AppError.InvalidFeed
        val items = mutableListOf<RssEpisode>()
        val channel = readTree(xml) { item -> item.toEpisode()?.let(items::add) }.find("channel") ?: Node("channel")
        // The feed's cover can come after the items, so it fills in the episodes without their own at the end.
        val channelImage = channel.child("itunes:image")?.attributes?.get("href").webUrl()
            ?: channel.child("image")?.text("url").webUrl()
        val episodes = items.map { if (it.imageUrl == null) it.copy(imageUrl = channelImage) else it }
        AppLogger.d(TAG, "Finished XML parse. Total episodes: ${episodes.size}")

        return RssFeed(
            title = channel.text("title").orEmpty(),
            description = channel.text("description") ?: channel.text("itunes:summary").orEmpty(),
            imageUrl = channelImage,
            author = channel.text("itunes:author"),
            language = channel.text("language"),
            categories = channel.categories(),
            link = channel.text("link").webUrl(),
            ttl = channel.text("ttl")?.toIntOrNull(),
            episodes = episodes,
            selfUrl = channel.children("atom:link").firstOrNull { it.attributes["rel"] == "self" }
                ?.attributes?.get("href").webUrl(),
            newFeedUrl = channel.text("itunes:new-feed-url").webUrl(),
            explicit = channel.text("itunes:explicit")?.lowercase() in EXPLICIT_VALUES,
            serial = channel.text("itunes:type")?.lowercase() == "serial",
        )
    }

    // An item without audio (a blog post in the same feed) is not an episode.
    private fun Node.toEpisode(): RssEpisode? {
        val enclosure = child("enclosure")?.attributes
        val enclosureUrl = enclosure?.get("url").webUrl() ?: return null
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
            link = text("link").webUrl(),
            enclosureType = enclosure?.get("type"),
            duration = text("itunes:duration"),
            publishDate = text("pubDate").orEmpty(),
            imageUrl = child("itunes:image")?.attributes?.get("href").webUrl(),
            explicit = text("itunes:explicit")?.lowercase() in EXPLICIT_VALUES,
            season = text("itunes:season")?.toIntOrNull(),
            episode = text("itunes:episode")?.toIntOrNull(),
            episodeType = text("itunes:episodeType"),
            chaptersUrl = chaptersUrl(),
            transcript = transcript(),
        )
    }

    // Only the JSON chapters (19.1); a type is required by the spec, but some feeds leave it out.
    private fun Node.chaptersUrl(): String? = children("podcast:chapters")
        .firstOrNull { it.attributes["type"]?.contains("json") ?: true }
        ?.attributes?.get("url").webUrl()

    /**
     * A transcript the player can follow (19.2): WebVTT first, then SRT, then the Podcasting 2.0 JSON. HTML and plain
     * text have no timing, so they are left out.
     */
    private fun Node.transcript(): RssTranscript? = children("podcast:transcript")
        .mapNotNull { node ->
            val url = node.attributes["url"].webUrl() ?: return@mapNotNull null
            val type = node.attributes["type"]?.trim()?.lowercase().orEmpty()
            TRANSCRIPT_TYPES.indexOfFirst { it in type }.takeIf { it >= 0 }?.let { it to RssTranscript(url, type) }
        }
        .minByOrNull { it.first }?.second

    // Apple nests subcategories (`Technology > Software How-To`); both levels count.
    private fun Node.categories(): List<String> = children("itunes:category").flatMap { category ->
        listOfNotNull(category.attributes["text"]) + category.categories()
    }.distinct()

    /** The document's tree, without the channel's items: each one goes to [onItem] when it closes. */
    private fun readTree(xml: String, onItem: (Node) -> Unit): Node {
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
                    if (stack.size > MAX_DEPTH) throw AppError.InvalidFeed
                    val node = Node(reader.qualifiedName(), attributes)
                    stack.last().children += node
                    stack.addLast(node)
                }
                EventType.END_ELEMENT -> {
                    val node = stack.removeLast()
                    if (node.name == "item" && stack.last().name == "channel") {
                        stack.last().children.removeLast()
                        onItem(node)
                    }
                }
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
        in PODCAST_NS -> "podcast:$localName"
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

/**
 * Whether [xml] declares entities before its [root] element, where a DOCTYPE lives. The reader expands declared
 * entities without bound, so a few KB of nested ones fill the memory (the billion laughs, 22.3); feeds and OPML files
 * never declare any.
 */
internal fun declaresEntities(xml: String, root: String): Boolean = "<!ENTITY" in xml.substringBefore(root)

/**
 * Only a web address, trimmed: the player, the image loader and iOS's URLSession would also open `file:`, `content:`
 * and the like, so a feed could point them at the app's own files (22.3, T4 of ADR 0014).
 */
internal fun String?.webUrl(): String? = this?.trim()?.takeIf { url ->
    WEB_SCHEMES.any { url.startsWith(it, ignoreCase = true) }
}

private val WEB_SCHEMES = listOf("https://", "http://")
