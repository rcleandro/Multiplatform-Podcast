package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory

/** Where podcasts are found by name, author or subject (18.1), and the most popular ones per category (18.2). */
interface PodcastDirectory {
    suspend fun search(term: String): Result<List<DirectoryPodcast>>

    /** The chart of the device's country, most popular first; all categories when [category] is null. */
    suspend fun top(category: PodcastCategory?): Result<List<DirectoryPodcast>>
}
