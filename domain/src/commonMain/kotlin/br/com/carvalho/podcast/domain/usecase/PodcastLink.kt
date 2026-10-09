package br.com.carvalho.podcast.domain.usecase

// Podcast apps' own schemes for a feed; `podcasts` is Apple's, followed by an Apple Podcasts address.
private val FEED_SCHEMES = setOf("podcast", "podcasts", "pcast", "itpc", "feed")
private val APPLE_HOSTS = setOf("podcasts.apple.com", "itunes.apple.com")
private val APPLE_ID = Regex("""/id(\d+)""")

/** Where a link opened in the app leads (18.4). */
sealed interface PodcastLink {
    data class Feed(val url: String) : PodcastLink

    /** A show in Apple Podcasts, whose feed the directory knows. */
    data class Apple(val id: Long) : PodcastLink
}

/**
 * What [link] points to: a feed address, as is or under a podcast app scheme (`pcast://host/feed`, also before a full
 * address: `feed:https://host/feed`), or an Apple Podcasts show (`…/podcast/name/id123`). `null` when it is neither.
 */
fun podcastLink(link: String): PodcastLink? {
    val trimmed = link.trim()
    val url = if (trimmed.substringBefore(':', "").lowercase() in FEED_SCHEMES) {
        trimmed.substringAfter(':').removePrefix("//").let { if (it.isWebAddress()) it else "https://$it" }
    } else {
        trimmed.takeIf { it.isWebAddress() } ?: return null
    }
    val afterScheme = url.substringAfter("://")
    val host = afterScheme.takeWhile { it !in "/?#" }.lowercase()
    return if (host in APPLE_HOSTS) {
        APPLE_ID.find(afterScheme.drop(host.length))?.groupValues?.get(1)?.toLongOrNull()?.let(PodcastLink::Apple)
    } else {
        validFeedUrl(url)?.let(PodcastLink::Feed)
    }
}

private fun String.isWebAddress() =
    startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
