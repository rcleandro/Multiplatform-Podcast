package br.com.carvalho.podcast.data.mapper

import io.ktor.http.Url
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.episodeId
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.data.remote.model.RssEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional

private const val TAG = "RssMapper"

fun RssFeed.toPodcast(feedUrl: String): Podcast = Podcast(
    id = feedUrl,
    title = title.ifBlank { Url(feedUrl).host.ifBlank { feedUrl } },
    description = description,
    imageUrl = imageUrl,
    author = author,
    language = language,
    categories = categories,
    feedUrl = feedUrl,
    siteUrl = link,
    lastUpdated = getCurrentTimestamp(),
    isSubscribed = true,
    episodeCount = episodes.size,
    feedVersion = version,
    // Only kept for a new podcast: refreshing an existing one does not rewrite this column.
    subscribedAt = getCurrentTimestamp(),
)


fun RssEpisode.toEpisode(podcastId: String, podcastTitle: String? = null): Episode = Episode(
    id = episodeId(podcastId, guid, enclosureUrl),
    podcastId = podcastId,
    podcastTitle = podcastTitle,
    title = title,
    description = description,
    audioUrl = enclosureUrl,
    imageUrl = imageUrl,
    duration = parseDuration(duration),
    publishDate = parsePubDate(publishDate),
    isPlayed = false,
    playbackPosition = 0,
    isDownloaded = false,
    fileSize = null
)

// RFC 822 dates as feeds write them: weekday optional (and often wrong, so it is dropped), seconds optional.
private val PUB_DATE_FORMAT = DateTimeComponents.Format {
    day(Padding.NONE)
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    year()
    char(' ')
    hour()
    char(':')
    minute()
    optional {
        char(':')
        second()
    }
    char(' ')
    offset(UtcOffset.Formats.FOUR_DIGITS)
}

// RFC 822 zone names; anything else must be a numeric offset. No zone at all is read as UTC.
private val ZONE_OFFSETS = mapOf(
    "GMT" to "+0000", "UT" to "+0000", "UTC" to "+0000", "Z" to "+0000",
    "EST" to "-0500", "EDT" to "-0400", "CST" to "-0600", "CDT" to "-0500",
    "MST" to "-0700", "MDT" to "-0600", "PST" to "-0800", "PDT" to "-0700",
)
private const val PARTS_WITHOUT_ZONE = 4

private fun parsePubDate(pubDate: String): Long {
    if (pubDate.isBlank()) return 0L
    return try {
        val parts = pubDate.substringAfter(',').trim().split(Regex("\\s+"))
        val zone = parts.getOrNull(PARTS_WITHOUT_ZONE)
        val offset = zone?.let { ZONE_OFFSETS[it.uppercase()] ?: it } ?: "+0000"
        val normalized = (parts.take(PARTS_WITHOUT_ZONE) + offset).joinToString(" ")
        PUB_DATE_FORMAT.parse(normalized).toInstantUsingOffset().toEpochMilliseconds()
    } catch (e: IllegalArgumentException) {
        AppLogger.e(TAG, "Failed to parse pubDate: '$pubDate'", e)
        0L
    }
}

private const val MAX_DURATION_PARTS = 3
private const val SECONDS_PER_MINUTE = 60

private fun parseDuration(duration: String?): Long {
    if (duration == null) return 0
    return try {
        if (duration.contains(":")) {
            // "mm:ss" or "hh:mm:ss": each part counts sixty of the next one.
            val parts = duration.split(":").map { it.trim().toLong() }
            if (parts.size > MAX_DURATION_PARTS) 0
            else parts.fold(0L) { total, part -> total * SECONDS_PER_MINUTE + part }
        } else {
            duration.toLong()
        }
    } catch (e: NumberFormatException) {
        AppLogger.e(TAG, "Failed to parse duration: '$duration'", e)
        0L
    }
}
