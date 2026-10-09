package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory

/**
 * Where podcasts are found by name, author or subject (18.1), the most popular ones per category (18.2) and a show by
 * its Apple Podcasts id (18.4).
 */
interface PodcastDirectory {
    suspend fun search(term: String): Result<List<DirectoryPodcast>>

    /** The chart of the device's country, most popular first; all categories when [category] is null. */
    suspend fun top(category: PodcastCategory?): Result<List<DirectoryPodcast>>

    /** The show with this Apple Podcasts [id] (18.4); `null` when there is none or it has no public feed. */
    suspend fun lookup(id: Long): Result<DirectoryPodcast?>
}
