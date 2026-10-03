package br.com.carvalho.podcast.domain.model

sealed class PodcastError : Exception() {
    object AlreadyExists : PodcastError()
    object FetchFailed : PodcastError()
    object ParseFailed : PodcastError()
}
