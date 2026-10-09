package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.DirectoryPodcast

/** Where podcasts are found by name, author or subject (18.1). */
interface PodcastDirectory {
    suspend fun search(term: String): Result<List<DirectoryPodcast>>
}
