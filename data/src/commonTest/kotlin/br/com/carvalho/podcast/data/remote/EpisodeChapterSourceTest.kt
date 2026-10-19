package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Chapter
import br.com.carvalho.podcast.domain.model.Episode
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class EpisodeChapterSourceTest {
    private val fileSystem = FakeFileSystem()
    private val requested = mutableListOf<String>()
    private var responses: Map<String, Any> = emptyMap()

    private val source = EpisodeChapterSource(
        HttpClient(
            MockEngine { request ->
                val url = request.url.toString()
                requested += url
                when (val body = responses[url]) {
                    is String -> respond(body)
                    is ByteArray -> respond(body)
                    else -> respondError(HttpStatusCode.NotFound)
                }
            }
        ),
        Json { ignoreUnknownKeys = true },
        fileSystem,
        CoroutineDispatchers(main = Dispatchers.Unconfined, io = Dispatchers.Unconfined),
    )

    @Test
    fun theFeedsJsonChaptersComeInStartOrderWithoutTheHiddenOnes() = runTest {
        responses = mapOf(
            CHAPTERS_URL to """
                {"version":"1.2.0","chapters":[
                  {"startTime":61.5,"title":" Second ","img":"https://example.com/2.jpg"},
                  {"startTime":30,"title":"Art only","toc":false},
                  {"startTime":0,"title":"First"}
                ]}
            """.trimIndent(),
        )

        val chapters = source.chapters(episode(chaptersUrl = CHAPTERS_URL))

        assertEquals(listOf(Chapter(0, "First"), Chapter(61_500, "Second")), chapters)
        assertEquals(listOf(CHAPTERS_URL), requested)
    }

    @Test
    fun withoutJsonTheChaptersComeFromTheStreamsId3Tag() = runTest {
        // The JSON is gone: the audio's own chapters are read instead.
        responses = mapOf(AUDIO_URL to id3Tag(4, chapterFrame(4, "a", 0, title(4, "Intro"))) + ByteArray(AUDIO_BYTES))

        val chapters = source.chapters(episode(chaptersUrl = CHAPTERS_URL))

        assertEquals(listOf(Chapter(0, "Intro")), chapters)
        assertEquals(listOf(CHAPTERS_URL, AUDIO_URL), requested)
    }

    @Test
    fun aDownloadedEpisodeReadsItsFileInsteadOfTheNetwork() = runTest {
        fileSystem.write(LOCAL_PATH.toPath()) { write(id3Tag(3, chapterFrame(3, "a", 7, title(3, "Local")))) }

        val chapters = source.chapters(episode(localPath = LOCAL_PATH))

        assertEquals(listOf(Chapter(7, "Local")), chapters)
        assertEquals(emptyList(), requested)
    }

    @Test
    fun audioWithoutAnId3TagOrThatFailsHasNoChapters() = runTest {
        responses = mapOf(AUDIO_URL to "ftypM4A not an mp3".encodeToByteArray())
        assertEquals(emptyList(), source.chapters(episode()))

        responses = mapOf(CHAPTERS_URL to "{ not json")
        assertEquals(emptyList(), source.chapters(episode(chaptersUrl = CHAPTERS_URL)))

        assertEquals(emptyList(), source.chapters(episode(localPath = "/missing.mp3")))
    }

    private fun episode(chaptersUrl: String? = null, localPath: String? = null) = Episode(
        id = "e1",
        podcastId = "p1",
        title = "Episode",
        description = null,
        audioUrl = AUDIO_URL,
        imageUrl = null,
        duration = 0,
        publishDate = 0,
        isPlayed = false,
        playbackPosition = 0,
        isDownloaded = localPath != null,
        fileSize = null,
        localPath = localPath,
        chaptersUrl = chaptersUrl,
    )

    private companion object {
        const val CHAPTERS_URL = "https://example.com/chapters.json"
        const val AUDIO_URL = "https://cdn.example.com/episode.mp3"
        const val LOCAL_PATH = "/episode.mp3"
        const val AUDIO_BYTES = 4_096
    }
}
