package br.com.carvalho.podcast.feature.podcast.presentation

/**
 * The season whose header goes above the episode at [index], or null when it continues the season above it (18.10).
 * Episodes without a season (a show's trailer, say) neither get a header nor break the season they sit in.
 */
fun seasonStartingAt(index: Int, seasonAt: (Int) -> Int?): Int? {
    val season = seasonAt(index) ?: return null
    val previous = (index - 1 downTo 0).asSequence().mapNotNull(seasonAt).firstOrNull()
    return season.takeIf { it != previous }
}
