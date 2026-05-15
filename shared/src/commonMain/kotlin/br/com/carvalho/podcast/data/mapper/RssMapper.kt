package br.com.carvalho.podcast.data.mapper

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.data.remote.model.RssEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock

private const val TAG = "RssMapper"

fun RssFeed.toPodcast(feedUrl: String): Podcast = Podcast(
    id = feedUrl,
    title = title,
    description = description,
    imageUrl = imageUrl,
    author = author,
    language = language,
    categories = categories,
    feedUrl = feedUrl,
    siteUrl = link,
    lastUpdated = Clock.System.now().toEpochMilliseconds(),
    isSubscribed = true,
    episodeCount = episodes.size
)

fun RssEpisode.toEpisode(podcastId: String, podcastTitle: String? = null): Episode = Episode(
    id = guid,
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

private fun parsePubDate(pubDate: String): Long {
    return try {
        val parts = pubDate.split(" ").filter { it.isNotEmpty() }
        if (parts.size < 4) return 0L

        val dayIdx = if (parts[0].contains(",")) 1 else 0

        val day = parts[dayIdx].toInt()
        val monthStr = parts[dayIdx + 1]
        val year = parts[dayIdx + 2].toInt()
        val timeParts = parts[dayIdx + 3].split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val second = if (timeParts.size > 2) timeParts[2].toInt() else 0

        val month = when (monthStr.uppercase()) {
            "JAN" -> 1
            "FEB" -> 2
            "MAR" -> 3
            "APR" -> 4
            "MAY" -> 5
            "JUN" -> 6
            "JUL" -> 7
            "AUG" -> 8
            "SEP" -> 9
            "OCT" -> 10
            "NOV" -> 11
            "DEC" -> 12
            else -> 1
        }

        val localDateTime = LocalDateTime(year, month, day, hour, minute, second)
        localDateTime.toInstant(TimeZone.UTC).toEpochMilliseconds()
    } catch (e: Exception) {
        AppLogger.e(TAG, "Failed to parse pubDate: '$pubDate'", e)
        0L
    }
}

private fun parseDuration(duration: String?): Long {
    if (duration == null) return 0
    return try {
        if (duration.contains(":")) {
            val parts = duration.split(":").map { it.trim().toLong() }
            when (parts.size) {
                2 -> parts[0] * 60 + parts[1]
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                else -> 0
            }
        } else {
            duration.toLong()
        }
    } catch (e: Exception) {
        AppLogger.e(TAG, "Failed to parse duration: '$duration'", e)
        0L
    }
}
