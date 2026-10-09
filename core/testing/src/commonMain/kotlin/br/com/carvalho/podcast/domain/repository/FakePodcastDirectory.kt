package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.DirectoryPodcast

class FakePodcastDirectory : PodcastDirectory {
    var result: Result<List<DirectoryPodcast>> = Result.success(emptyList())
    val searchedTerms = mutableListOf<String>()

    override suspend fun search(term: String): Result<List<DirectoryPodcast>> {
        searchedTerms += term
        return result
    }
}
