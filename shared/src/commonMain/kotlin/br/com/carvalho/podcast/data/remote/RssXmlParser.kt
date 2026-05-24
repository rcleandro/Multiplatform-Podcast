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

        var channelImage = extractAttribute(channelXml, "itunes:image", "href")
        if (channelImage == null) {
            channelImage = extractTag(channelXml, "url")
        }

        val channelAuthor = extractTag(channelXml, "itunes:author") ?: "Autor Desconhecido"

        val episodes = mutableListOf<RssEpisode>()
        var currentPos = firstItemPos

        while (currentPos != -1) {
            val startItem = xml.indexOf("<item>", currentPos)
            if (startItem == -1) break
            
            val endItem = xml.indexOf("</item>", startItem)
            if (endItem == -1) break
            
            val itemXml = xml.substring(startItem, endItem + 7)

            val title = extractTag(itemXml, "title") ?: "Sem título"
            val guid = extractTag(itemXml, "guid") ?: title.hashCode().toString()
            val enclosureUrl = extractAttribute(itemXml, "enclosure", "url") ?: ""
            val imageUrl = extractAttribute(itemXml, "itunes:image", "href") ?: channelImage
            val duration = extractTag(itemXml, "itunes:duration")
            val pubDate = extractTag(itemXml, "pubDate") ?: ""

            episodes.add(
                RssEpisode(
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
            )

            currentPos = endItem + 7
        }

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

    private fun extractTag(xml: String, tagName: String): String? {
        val startTagPattern = "<$tagName"
        var startIndex = xml.indexOf(startTagPattern)
        
        // Verify it's actually the tag name and not a prefix (e.g. <tag vs <tagName)
        while (startIndex != -1) {
            val nextChar = xml.getOrNull(startIndex + startTagPattern.length)
            if (nextChar == '>' || nextChar == ' ' || nextChar == '\t' || nextChar == '\r' || nextChar == '\n') {
                break
            }
            startIndex = xml.indexOf(startTagPattern, startIndex + 1)
        }
        
        if (startIndex == -1) return null
        
        val startTagEnd = xml.indexOf(">", startIndex)
        if (startTagEnd == -1) return null
        
        val endTag = "</$tagName>"
        val endIndex = xml.indexOf(endTag, startTagEnd)
        if (endIndex == -1) return null
        
        val content = xml.substring(startTagEnd + 1, endIndex).trim()
        
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
