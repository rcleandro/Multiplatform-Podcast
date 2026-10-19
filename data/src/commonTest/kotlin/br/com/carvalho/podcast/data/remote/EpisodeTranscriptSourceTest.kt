package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.TranscriptLine
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class EpisodeTranscriptSourceTest {
    private val source = EpisodeTranscriptSource(
        HttpClient(
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/1.vtt" -> respond("WEBVTT\n\n00:00.000 --> 00:01.000\nHi")
                    "/1.json" -> respond("""{"segments":[{"startTime":0,"endTime":1,"body":"Hi."}]}""")
                    else -> respondError(HttpStatusCode.NotFound)
                }
            }
        ),
        Json { ignoreUnknownKeys = true },
        CoroutineDispatchers(main = Dispatchers.Unconfined, io = Dispatchers.Unconfined, default = Dispatchers.Unconfined),
    )

    @Test
    fun theTypeTheFeedGaveDecidesHowItIsRead() = runTest {
        val line = listOf(TranscriptLine(0, 1_000, "Hi"))
        assertEquals(line, source.transcript(episode("https://example.com/1.vtt", "text/vtt")).getOrThrow())
        assertEquals(
            listOf(TranscriptLine(0, 1_000, "Hi.")),
            source.transcript(episode("https://example.com/1.json", "application/json")).getOrThrow(),
        )
    }

    @Test
    fun aMissingTranscriptFailsWithTheServersAnswer() = runTest {
        val result = source.transcript(episode("https://example.com/gone.vtt", "text/vtt"))

        assertEquals(AppError.Http(HttpStatusCode.NotFound.value), result.exceptionOrNull())
    }

    private fun episode(url: String, type: String) = Episode(
        id = "e1", podcastId = "p1", title = "Episode", description = null, audioUrl = "https://example.com/1.mp3",
        imageUrl = null, duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false,
        fileSize = null, transcriptUrl = url, transcriptType = type,
    )
}
