package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.deviceCountry
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.repository.PodcastDirectory
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val ITUNES = "https://itunes.apple.com"
private const val RESULT_LIMIT = 30
private const val CHART_LIMIT = 25
private const val FALLBACK_COUNTRY = "US"

/**
 * Apple's iTunes Search API and charts: free, no key, and they answer browsers too (CORS). The country decides the
 * catalog and the language of the genres, so it follows the device. The body comes as `text/javascript`, hence the
 * manual decode.
 */
class ITunesPodcastDirectory(
    private val client: HttpClient,
    private val json: Json,
    private val country: () -> String? = ::deviceCountry,
) : PodcastDirectory {
    override suspend fun search(term: String): Result<List<DirectoryPodcast>> = catchingAppError {
        val response = client.get("$ITUNES/search") {
            parameter("media", "podcast")
            parameter("entity", "podcast")
            parameter("term", term)
            parameter("limit", RESULT_LIMIT)
            parameter("country", countryCode())
        }
        decode<SearchResponse>(response).results.mapNotNull { it.toDirectoryPodcast() }
    }

    /**
     * The chart only names each podcast's id; one lookup brings their feeds, in its own order, so the chart's order is
     * put back. The chart's JSON holds a single entry as an object instead of a list.
     */
    override suspend fun top(category: PodcastCategory?): Result<List<DirectoryPodcast>> = catchingAppError {
        val genre = category?.let { "/genre=${it.genreId}" }.orEmpty()
        val chart = client.get("$ITUNES/${countryCode().lowercase()}/rss/toppodcasts/limit=$CHART_LIMIT$genre/json")
        val ids = chartIds(decode<JsonObject>(chart))
        if (ids.isEmpty()) return@catchingAppError emptyList()
        val lookup = client.get("$ITUNES/lookup") {
            parameter("id", ids.joinToString(","))
            parameter("entity", "podcast")
            parameter("country", countryCode())
        }
        val found = decode<SearchResponse>(lookup).results.associateBy { it.collectionId }
        ids.mapNotNull { found[it]?.toDirectoryPodcast() }
    }

    override suspend fun lookup(id: Long): Result<DirectoryPodcast?> = catchingAppError {
        val response = client.get("$ITUNES/lookup") {
            parameter("id", id)
            parameter("entity", "podcast")
            parameter("country", countryCode())
        }
        decode<SearchResponse>(response).results.firstOrNull()?.toDirectoryPodcast()
    }

    private fun countryCode() = country() ?: FALLBACK_COUNTRY

    private suspend inline fun <reified T> decode(response: HttpResponse): T {
        if (!response.status.isSuccess()) throw AppError.Http(response.status.value)
        return json.decodeFromString<T>(response.bodyAsText())
    }

    private fun chartIds(chart: JsonObject): List<Long> {
        val entries = when (val entry = chart["feed"]?.jsonObject?.get("entry")) {
            is JsonArray -> entry
            is JsonObject -> listOf(entry)
            else -> emptyList()
        }
        return entries.mapNotNull { entry ->
            val attributes = entry.jsonObject["id"]?.jsonObject?.get("attributes")?.jsonObject
            attributes?.get("im:id")?.jsonPrimitive?.content?.toLongOrNull()
        }
    }

    @Serializable
    private data class SearchResponse(val results: List<Entry> = emptyList())

    @Serializable
    private data class Entry(
        val collectionId: Long? = null,
        val collectionName: String? = null,
        val artistName: String? = null,
        val feedUrl: String? = null,
        val artworkUrl600: String? = null,
        val artworkUrl100: String? = null,
        val primaryGenreName: String? = null,
        val trackCount: Int? = null,
        // "explicit", "cleaned" or "notExplicit". The collection's own field says "notExplicit" even for explicit
        // shows (checked on 09/10/2026), so the track's is the one that tells.
        val trackExplicitness: String? = null,
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
            explicit = trackExplicitness == "explicit",
        )
    }
}
