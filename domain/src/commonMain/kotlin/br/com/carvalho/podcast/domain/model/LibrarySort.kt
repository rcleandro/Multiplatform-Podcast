package br.com.carvalho.podcast.domain.model

/** Orders the library offers. */
enum class LibrarySort { TITLE, RECENTLY_ADDED, FIRST_ADDED, LATEST_EPISODE, MOST_UNPLAYED }

private val byTitle = compareBy<LibraryEntry> { it.podcast.title.lowercase() }

/** [sort] applied to the library; ties fall back to the title, and podcasts without episodes go last. */
fun List<LibraryEntry>.sortedFor(sort: LibrarySort): List<LibraryEntry> = when (sort) {
    LibrarySort.TITLE -> sortedWith(byTitle)
    LibrarySort.RECENTLY_ADDED ->
        sortedWith(compareByDescending<LibraryEntry> { it.podcast.subscribedAt }.then(byTitle))
    LibrarySort.FIRST_ADDED -> sortedWith(compareBy<LibraryEntry> { it.podcast.subscribedAt }.then(byTitle))
    LibrarySort.LATEST_EPISODE ->
        sortedWith(compareByDescending<LibraryEntry> { it.latestEpisodeDate ?: Long.MIN_VALUE }.then(byTitle))
    LibrarySort.MOST_UNPLAYED -> sortedWith(compareByDescending<LibraryEntry> { it.unplayedCount }.then(byTitle))
}
