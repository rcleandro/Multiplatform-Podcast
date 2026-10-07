package br.com.carvalho.podcast.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LibrarySortTest {
    private fun entry(title: String, subscribedAt: Long, latest: Long?, unplayed: Int) = LibraryEntry(
        Podcast(
            id = title, title = title, description = "", imageUrl = null, author = null, language = null,
            categories = emptyList(), feedUrl = title, siteUrl = null, lastUpdated = 0, isSubscribed = true,
            subscribedAt = subscribedAt,
        ),
        unplayedCount = unplayed,
        latestEpisodeDate = latest,
    )

    private val entries = listOf(
        entry("beta", subscribedAt = 2, latest = 50, unplayed = 3),
        entry("Alpha", subscribedAt = 3, latest = null, unplayed = 3),
        entry("gamma", subscribedAt = 1, latest = 90, unplayed = 7),
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
    }
}
