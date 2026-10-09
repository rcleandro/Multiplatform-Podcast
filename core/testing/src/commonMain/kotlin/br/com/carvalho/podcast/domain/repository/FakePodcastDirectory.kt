package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory

class FakePodcastDirectory : PodcastDirectory {
    var result: Result<List<DirectoryPodcast>> = Result.success(emptyList())
    val searchedTerms = mutableListOf<String>()
    var topResult: Result<List<DirectoryPodcast>> = Result.success(emptyList())
    val chartsAsked = mutableListOf<PodcastCategory?>()
    var lookupResult: Result<DirectoryPodcast?> = Result.success(null)
    val lookedUp = mutableListOf<Long>()

    override suspend fun search(term: String): Result<List<DirectoryPodcast>> {
        searchedTerms += term
        return result
    }

    override suspend fun top(category: PodcastCategory?): Result<List<DirectoryPodcast>> {
        chartsAsked += category
        return topResult
    }

    override suspend fun lookup(id: Long): Result<DirectoryPodcast?> {
        lookedUp += id
        return lookupResult
    }
}
