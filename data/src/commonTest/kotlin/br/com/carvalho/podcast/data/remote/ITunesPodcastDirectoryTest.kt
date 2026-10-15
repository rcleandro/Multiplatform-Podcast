package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ITunesPodcastDirectoryTest {
    private val json = Json { ignoreUnknownKeys = true }
    private var sent: HttpRequestData? = null

    private fun directory(status: HttpStatusCode = HttpStatusCode.OK, country: String? = "BR") = ITunesPodcastDirectory(
        HttpClient(
            MockEngine {
                sent = it
                respond(RESPONSE, status, headersOf(HttpHeaders.ContentType, "text/javascript; charset=utf-8"))
            }
        ),
        json,
        country = { country },
    )

    @Test
    fun resultsWithAFeedBecomeDirectoryPodcasts() = runTest {
        val found = directory().search("hipsters").getOrThrow()

        assertEquals(
            DirectoryPodcast(
                title = "Hipsters Ponto Tech",
                author = "Alura - Hipsters Network",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/03/02/dc/600x600bb.jpg",
                feedUrl = "https://hipsters.tech/feed/podcast/",
                genre = "Tecnologia",
                episodeCount = 629,
            ),
            found.first(),
        )
        // The track's field tells, even when the collection's says "notExplicit".
        assertEquals(listOf(false, true), found.map { it.explicit })
        // The Apple-only show has no feed to subscribe to.
        assertEquals(listOf("Hipsters Ponto Tech", "IA Sob Controle - Inteligência Artificial"), found.map { it.title })
    }

    @Test
    fun theSearchAsksForPodcastsInTheDeviceCountry() = runTest {
        directory().search("café com dev")

        val url = sent!!.url
        assertEquals("itunes.apple.com", url.host)
        assertEquals("café com dev", url.parameters["term"])
        assertEquals("podcast", url.parameters["media"])
        assertEquals("BR", url.parameters["country"])
    }

    @Test
    fun withoutACountryTheUsCatalogIsUsed() = runTest {
        directory(country = null).search("news")

        assertEquals("US", sent!!.url.parameters["country"])
    }

    @Test
    fun aServerErrorIsReported() = runTest {
        val result = directory(status = HttpStatusCode.ServiceUnavailable).search("news")

        assertEquals(AppError.Http(HttpStatusCode.ServiceUnavailable.value), result.exceptionOrNull())
    }

    private val requests = mutableListOf<HttpRequestData>()

    private fun chartDirectory(chart: String) = ITunesPodcastDirectory(
        HttpClient(
            MockEngine {
                requests += it
                val body = if (it.url.encodedPath.endsWith("/json")) chart else LOOKUP
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/javascript; charset=utf-8"))
            }
        ),
        json,
        country = { "BR" },
    )

    @Test
    fun theChartOfACategoryKeepsItsOrderAfterTheLookup() = runTest {
        val top = chartDirectory(CHART).top(PodcastCategory.TECHNOLOGY).getOrThrow()

        // The lookup answers in another order; the chart's is what matters.
        assertEquals(listOf("MacMagazine no Ar", "Hipsters Ponto Tech"), top.map { it.title })
        assertEquals("/br/rss/toppodcasts/limit=25/genre=1318/json", requests[0].url.encodedPath)
        assertEquals("547137017,1133325943", requests[1].url.parameters["id"])
        assertEquals("BR", requests[1].url.parameters["country"])
    }

    @Test
    fun aChartWithOneEntryAndTheOverallChartAreRead() = runTest {
        val top = chartDirectory(SINGLE_ENTRY_CHART).top(category = null).getOrThrow()

        assertEquals(listOf("Hipsters Ponto Tech"), top.map { it.title })
        assertEquals("/br/rss/toppodcasts/limit=25/json", requests[0].url.encodedPath)
    }

    @Test
    fun anEmptyChartNeedsNoLookup() = runTest {
        val top = chartDirectory("""{ "feed": { "author": { "name": { "label": "iTunes Store" } } } }""").top(null)

        assertEquals(emptyList(), top.getOrThrow())
        assertEquals(1, requests.size)
    }

    @Test
    fun aShowIsLookedUpByItsId() = runTest {
        val found = chartDirectory(CHART).lookup(1_133_325_943).getOrThrow()

        assertEquals("Hipsters Ponto Tech", found?.title)
        assertEquals("/lookup", requests.single().url.encodedPath)
        assertEquals("1133325943", requests.single().url.parameters["id"])
        assertEquals("BR", requests.single().url.parameters["country"])
    }

    @Test
    fun anUnknownShowIsNotFound() = runTest {
        val found = ITunesPodcastDirectory(
            HttpClient(MockEngine { respond("""{ "resultCount": 0, "results": [] }""") }),
            json,
            country = { "BR" },
        ).lookup(1).getOrThrow()

        assertEquals(null, found)
    }

    private companion object {
        // Trimmed from a real answer of https://itunes.apple.com/search (08/10/2026), plus an entry without a feed.
        // Trimmed from https://itunes.apple.com/br/rss/toppodcasts/limit=2/genre=1318/json (09/10/2026).
        const val CHART = """
        { "feed": { "entry": [
          { "im:name": { "label": "MacMagazine no Ar" }, "id": { "attributes": { "im:id": "547137017" } } },
          { "im:name": { "label": "Hipsters Ponto Tech" }, "id": { "attributes": { "im:id": "1133325943" } } }
        ] } }
        """
        const val SINGLE_ENTRY_CHART = """
        { "feed": { "entry":
          { "im:name": { "label": "Hipsters Ponto Tech" }, "id": { "attributes": { "im:id": "1133325943" } } }
        } }
        """
        const val LOOKUP = """
        { "resultCount": 2, "results": [
          { "kind": "podcast", "collectionId": 1133325943, "collectionName": "Hipsters Ponto Tech",
            "artistName": "Alura - Hipsters Network", "feedUrl": "https://hipsters.tech/feed/podcast/" },
          { "kind": "podcast", "collectionId": 547137017, "collectionName": "MacMagazine no Ar",
            "artistName": "MacMagazine", "feedUrl": "https://macmagazine.com.br/feed/podcast/" }
        ] }
        """
        const val RESPONSE = """
        {
         "resultCount": 3,
         "results": [
          {
           "kind": "podcast", "collectionId": 1133325943,
           "collectionName": "Hipsters Ponto Tech", "artistName": "Alura - Hipsters Network",
           "feedUrl": "https://hipsters.tech/feed/podcast/",
           "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/03/02/dc/100x100bb.jpg",
           "artworkUrl600": "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/03/02/dc/600x600bb.jpg",
           "primaryGenreName": "Tecnologia", "trackCount": 629
          },
          {
           "kind": "podcast", "collectionId": 1763888661,
           "collectionName": "IA Sob Controle - Inteligência Artificial", "artistName": "Alura - Hipsters Network",
           "feedUrl": "https://anchor.fm/s/f9fb7ee0/podcast/rss",
           "artworkUrl600": "https://is1-ssl.mzstatic.com/image/thumb/Podcasts211/v4/74/4b/b4/600x600bb.jpg",
           "primaryGenreName": "Tecnologia", "trackCount": 293,
           "collectionExplicitness": "notExplicit", "trackExplicitness": "explicit"
          },
          { "kind": "podcast", "collectionName": "Apple Exclusive", "artistName": "Apple", "primaryGenreName": "Tecnologia" }
         ]
        }
        """
    }
}
