package br.com.carvalho.podcast.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LibrarySortTest {
    private fun entry(title: String, subscribedAt: Long, latest: Long?, unplayed: Int, position: Int = 0) = LibraryEntry(
        Podcast(
            id = title, title = title, description = "", imageUrl = null, author = null, language = null,
            categories = emptyList(), feedUrl = title, siteUrl = null, lastUpdated = 0, isSubscribed = true,
            subscribedAt = subscribedAt,
            position = position,
        ),
        unplayedCount = unplayed,
        latestEpisodeDate = latest,
    )

    private val entries = listOf(
        entry("beta", subscribedAt = 2, latest = 50, unplayed = 3, position = 1),
        entry("Alpha", subscribedAt = 3, latest = null, unplayed = 3, position = 2),
        entry("gamma", subscribedAt = 1, latest = 90, unplayed = 7, position = 0),
    )

    private fun order(sort: LibrarySort) = entries.sortedFor(sort).map { it.podcast.title }

    @Test
    fun eachSortOrdersTheLibrary() {
        assertEquals(listOf("Alpha", "beta", "gamma"), order(LibrarySort.TITLE))
        assertEquals(listOf("Alpha", "beta", "gamma"), order(LibrarySort.RECENTLY_ADDED))
        assertEquals(listOf("gamma", "beta", "Alpha"), order(LibrarySort.FIRST_ADDED))
        // A podcast with no episodes goes last.
        assertEquals(listOf("gamma", "beta", "Alpha"), order(LibrarySort.LATEST_EPISODE))
        // Ties fall back to the title.
        assertEquals(listOf("gamma", "Alpha", "beta"), order(LibrarySort.MOST_UNPLAYED))
        assertEquals(listOf("gamma", "beta", "Alpha"), order(LibrarySort.CUSTOM))
    }
}
