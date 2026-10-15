package br.com.carvalho.podcast.domain.model

/** `itunes:episodeType` (18.10): a regular episode, a trailer for the show or a season, or extra content. */
enum class EpisodeType {
    FULL, TRAILER, BONUS;

    companion object {
        /** What the feed writes; anything else, or nothing, is a regular episode. */
        fun of(value: String?): EpisodeType = when (value?.trim()?.lowercase()) {
            "trailer" -> TRAILER
            "bonus" -> BONUS
            else -> FULL
        }
    }
}
