package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.deviceCountry
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.repository.PodcastDirectory
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val SEARCH_URL = "https://itunes.apple.com/search"
private const val RESULT_LIMIT = 30
private const val FALLBACK_COUNTRY = "US"

/**
 * Apple's iTunes Search API: free, no key, and it answers browsers too (CORS). The country decides the catalog and
 * the language of the genres, so it follows the device. The body comes as `text/javascript`, hence the manual decode.
 */
class ITunesPodcastDirectory(
    private val client: HttpClient,
    private val json: Json,
    private val country: () -> String? = ::deviceCountry,
) : PodcastDirectory {
    override suspend fun search(term: String): Result<List<DirectoryPodcast>> = catchingAppError {
        val response = client.get(SEARCH_URL) {
            parameter("media", "podcast")
            parameter("entity", "podcast")
            parameter("term", term)
            parameter("limit", RESULT_LIMIT)
            parameter("country", country() ?: FALLBACK_COUNTRY)
        }
        if (!response.status.isSuccess()) throw AppError.Http(response.status.value)
        json.decodeFromString<SearchResponse>(response.bodyAsText()).results.mapNotNull { it.toDirectoryPodcast() }
    }

    @Serializable
    private data class SearchResponse(val results: List<Entry> = emptyList())

    @Serializable
    private data class Entry(
        val collectionName: String? = null,
        val artistName: String? = null,
        val feedUrl: String? = null,
        val artworkUrl600: String? = null,
        val artworkUrl100: String? = null,
        val primaryGenreName: String? = null,
        val trackCount: Int? = null,
    )

    // Some entries have no public feed (Apple-only shows): nothing to subscribe to.
    private fun Entry.toDirectoryPodcast(): DirectoryPodcast? {
        if (collectionName == null || feedUrl == null) return null
        return DirectoryPodcast(
            title = collectionName,
            author = artistName,
            artworkUrl = artworkUrl600 ?: artworkUrl100,
            feedUrl = feedUrl,
            genre = primaryGenreName,
            episodeCount = trackCount,
        )
    }
}
