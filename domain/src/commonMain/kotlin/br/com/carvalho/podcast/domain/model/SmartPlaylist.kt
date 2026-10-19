package br.com.carvalho.podcast.domain.model

import kotlinx.serialization.Serializable

/**
 * A filter of episodes saved under a name (19.4): "unheard under 30 min of Technology", "downloaded News". It holds
 * no episodes of its own; it lists whatever matches when it is opened, newest first.
 */
@Serializable
data class SmartPlaylist(
    val id: Long = 0,
    val name: String,
    /** Only the podcasts of this category; null for every podcast. */
    val category: PodcastCategory? = null,
    val unplayedOnly: Boolean = false,
    val downloadedOnly: Boolean = false,
    /** Only episodes the feed says last this long or less; null for any length. */
    val maxMinutes: Int? = null,
) {
    companion object {
        /** The longest-episode choices, in minutes. */
        @Suppress("MagicNumber") // the choices themselves
        val LENGTH_CHOICES = listOf(15, 30, 60)
    }
}
