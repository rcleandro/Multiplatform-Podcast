package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.data.remote.model.RssEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.core.util.AppLogger

private const val TAG = "RssXmlParser"
object RssXmlParser {
    fun parse(xml: String): RssFeed {
        AppLogger.d(TAG, "Starting XML parse (length: ${xml.length})")
        val firstItemPos = xml.indexOf("<item>")
        val channelXml = if (firstItemPos != -1) xml.substring(0, firstItemPos) else xml

        val channelTitle = extractTag(channelXml, "title") ?: "Podcast Desconhecido"
        val channelDescription = extractTag(channelXml, "description") ?: ""
        val channelImage = extractChannelImage(channelXml)
        val channelAuthor = extractTag(channelXml, "itunes:author") ?: "Autor Desconhecido"

        val episodes = parseEpisodes(xml, firstItemPos, channelImage)

        AppLogger.d(TAG, "Finished XML parse. Total episodes: ${episodes.size}")

        return RssFeed(
            title = channelTitle,
            description = channelDescription,
            imageUrl = channelImage,
            author = channelAuthor,
            language = extractTag(xml, "language"),
            categories = emptyList(),
            link = extractTag(xml, "link"),
            ttl = null,
            episodes = episodes
        )
    }

    private fun extractChannelImage(channelXml: String): String? {
        return extractAttribute(channelXml, "itunes:image", "href") ?: extractTag(channelXml, "url")
    }

    private const val ITEM_TAG_LENGTH = 7

    private fun parseEpisodes(xml: String, firstItemPos: Int, defaultImage: String?): List<RssEpisode> {
        val episodes = mutableListOf<RssEpisode>()
        var currentPos = firstItemPos

        while (currentPos != -1) {
            val startItem = xml.indexOf("<item>", currentPos)
            val endItem = if (startItem != -1) xml.indexOf("</item>", startItem) else -1

            if (startItem == -1 || endItem == -1) {
                currentPos = -1
            } else {
                val itemXml = xml.substring(startItem, endItem + ITEM_TAG_LENGTH)
                episodes.add(parseEpisodeItem(itemXml, defaultImage))
                currentPos = endItem + ITEM_TAG_LENGTH
            }
        }
        return episodes
    }

    private fun parseEpisodeItem(itemXml: String, defaultImage: String?): RssEpisode {
        val title = extractTag(itemXml, "title") ?: "Sem título"
        val guid = extractTag(itemXml, "guid") ?: title.hashCode().toString()
        val enclosureUrl = extractAttribute(itemXml, "enclosure", "url") ?: ""
        val imageUrl = extractAttribute(itemXml, "itunes:image", "href") ?: defaultImage
        val duration = extractTag(itemXml, "itunes:duration")
        val pubDate = extractTag(itemXml, "pubDate") ?: ""

        return RssEpisode(
            guid = guid,
            title = title,
            description = extractTag(itemXml, "description"),
            enclosureUrl = enclosureUrl,
            enclosureType = "audio/mpeg",
            duration = duration,
            publishDate = pubDate,
            imageUrl = imageUrl,
            explicit = false,
            season = null,
            episode = null
        )
    }

    private fun extractTag(xml: String, tagName: String): String? {
        val startIndex = findTagStart(xml, tagName) ?: return null
        val startTagEnd = xml.indexOf(">", startIndex)
        if (startTagEnd == -1) return null

        val endTag = "</$tagName>"
        val endIndex = xml.indexOf(endTag, startTagEnd)
        if (endIndex == -1) return null

        val content = xml.substring(startTagEnd + 1, endIndex).trim()
        return cleanCData(content)
    }

    private fun findTagStart(xml: String, tagName: String): Int? {
        val startTagPattern = "<$tagName"
        var startIndex = xml.indexOf(startTagPattern)

        while (startIndex != -1) {
            val nextChar = xml.getOrNull(startIndex + startTagPattern.length)
            if (nextChar == null || nextChar == '>' || nextChar.isWhitespace()) {
                return startIndex
            }
            startIndex = xml.indexOf(startTagPattern, startIndex + 1)
        }
        return null
    }

    private fun cleanCData(content: String): String {
        return if (content.startsWith("<![CDATA[")) {
            content.removePrefix("<![CDATA[").removeSuffix("]]>").trim()
        } else {
            content
        }
    }

    private fun extractAttribute(xml: String, tagName: String, attributeName: String): String? {
        val tagStart = xml.indexOf("<$tagName")
        if (tagStart == -1) return null
        val tagEnd = xml.indexOf(">", tagStart)
        if (tagEnd == -1) return null
        val tagContent = xml.substring(tagStart, tagEnd)
        val attrStart = tagContent.indexOf("$attributeName=\"")
        val valueDelimiter = "\""
        var finalAttrStart = attrStart
        var finalDelimiter = valueDelimiter

        if (attrStart == -1) {
            finalAttrStart = tagContent.indexOf("$attributeName='")
            finalDelimiter = "'"
        }

        if (finalAttrStart == -1) return null

        val valueStart = finalAttrStart + attributeName.length + 2
        val valueEnd = tagContent.indexOf(finalDelimiter, valueStart)
        if (valueEnd == -1) return null
        return tagContent.substring(valueStart, valueEnd)
    }
}
