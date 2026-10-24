package br.com.carvalho.podcast.presentation.share

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import kotlin.test.Test
import kotlin.test.assertEquals

class ShareTextTest {
    private val podcast = Podcast(
        id = "p1", title = "Hipsters", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = "https://hipsters.tech/feed", siteUrl = "https://hipsters.tech",
        lastUpdated = 0, isSubscribed = true,
    )
    private val episode = Episode(
        id = "e1", podcastId = "p1", podcastTitle = "Hipsters", title = "Kotlin", description = null,
        audioUrl = "https://cdn.example.com/e1.mp3", imageUrl = null, duration = 0, publishDate = 0, isPlayed = false,
        playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    @Test
    fun aPodcastSharesItsSiteElseItsFeed() {
        assertEquals("Hipsters\nhttps://hipsters.tech", podcastShareText(podcast))
        assertEquals("Hipsters\nhttps://hipsters.tech/feed", podcastShareText(podcast.copy(siteUrl = null)))
    }

    @Test
    fun anEpisodeSharesItsPageElseItsAudio() {
        assertEquals(
            "Kotlin · Hipsters\nhttps://hipsters.tech/kotlin",
            episodeShareText(episode.copy(link = "https://hipsters.tech/kotlin")),
        )
        assertEquals("Kotlin · Hipsters\nhttps://cdn.example.com/e1.mp3", episodeShareText(episode))
    }

    @Test
    fun fromThePlayerTheMinuteGoesInTheTextAndInTheAudioLink() {
        assertEquals(
            "Kotlin · Hipsters\nFrom 12:34\nhttps://cdn.example.com/e1.mp3#t=754",
            episodeShareText(episode, startsAt = "From 12:34", positionMs = 754_900),
        )
        assertEquals(
            "Kotlin · Hipsters\nFrom 12:34\nhttps://hipsters.tech/kotlin",
            episodeShareText(episode.copy(link = "https://hipsters.tech/kotlin"), "From 12:34", 754_900),
        )
    }

    @Test
    fun aPrivateFeedIsSharedByNameAloneNeverByItsAddresses() {
        // Whoever gets the message would hear the paid feed (22.6): the feed and its audio carry the token.
        val private = "https://feeds.supercast.com/episodes/2c9f8b7a6d5e4f3a2b1c0d9e8f7a6b5c.rss"

        assertEquals("Hipsters", podcastShareText(podcast.copy(siteUrl = null, feedUrl = private)))
        assertEquals("Hipsters\nhttps://hipsters.tech", podcastShareText(podcast.copy(feedUrl = private)))
        assertEquals("Kotlin · Hipsters", episodeShareText(episode.copy(podcastId = private)))
        assertEquals("Kotlin · Hipsters", episodeShareText(episode.copy(audioUrl = "https://cdn.example.com/e1.mp3?token=abc")))
        assertEquals(
            "Kotlin · Hipsters\nhttps://hipsters.tech/kotlin",
            episodeShareText(episode.copy(podcastId = private, link = "https://hipsters.tech/kotlin")),
        )
    }
}
