package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.data.remote.model.RssFeed

interface RssFeedDataSource {
    suspend fun fetchFeed(url: String): Result<RssFeed>
    suspend fun validateFeedUrl(url: String): Result<Boolean>
}
