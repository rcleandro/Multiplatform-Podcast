package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Chapter
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.ChapterSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.utils.io.readByteArray
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

private const val TAG = "EpisodeChapterSource"
private const val MILLIS_PER_SECOND = 1000

/** Past this an ID3 tag is not read: it is mostly pictures, and the episode would be downloaded twice. */
private const val MAX_TAG_BYTES = 16 * 1024 * 1024

/**
 * The feed's JSON chapters (`podcast:chapters`) when it gives them and they load; otherwise the ID3 chapters at the
 * start of the audio, from the downloaded file or the first bytes of the stream. Read every time an episode loads.
 */
// ponytail: not cached, so a downloaded episode heard offline only has the chapters inside its file. Save them in the
// database if feeds with JSON chapters only turn out to be common.
class EpisodeChapterSource(
    private val client: HttpClient,
    private val json: Json,
    private val fileSystem: FileSystem,
    private val dispatchers: CoroutineDispatchers,
) : ChapterSource {

    override suspend fun chapters(episode: Episode): List<Chapter> = fromJson(episode).ifEmpty { fromId3(episode) }

    private suspend fun fromJson(episode: Episode): List<Chapter> {
        val url = episode.chaptersUrl ?: return emptyList()
        return reading("JSON", url) {
            val response = client.get(url)
            if (!response.status.isSuccess()) return@reading emptyList()
            json.decodeFromString<ChaptersJson>(response.bodyAsText()).chapters
                .filter { it.toc }
                .map { Chapter((it.startTime * MILLIS_PER_SECOND).toLong(), it.title.orEmpty().trim()) }
                .sortedBy { it.startMs }
        }
    }

    private suspend fun fromId3(episode: Episode): List<Chapter> {
        val local = episode.localPath
        if (local != null) {
            return reading("ID3", episode.audioUrl) {
                withContext(dispatchers.io) {
                    fileSystem.read(local.toPath()) { readTag { readByteArray(it.toLong()) } }
                }
            }
        }
        // Read as a stream and closed after the tag: the rest of the audio never comes down.
        return reading("ID3", episode.audioUrl) {
            client.prepareGet(episode.audioUrl).execute { response ->
                if (!response.status.isSuccess()) return@execute emptyList()
                val channel = response.bodyAsChannel()
                readTag { channel.readByteArray(it) }
            }
        }
    }

    private inline fun readTag(read: (Int) -> ByteArray): List<Chapter> {
        val header = read(Id3Chapters.HEADER_SIZE)
        val length = Id3Chapters.tagLength(header)?.takeIf { it <= MAX_TAG_BYTES } ?: return emptyList()
        return Id3Chapters.parse(header + read(length - Id3Chapters.HEADER_SIZE))
    }

    /** Chapters are extra: any failure leaves the episode without them. Only the host is logged (private feeds). */
    private inline fun reading(kind: String, url: String, block: () -> List<Chapter>): List<Chapter> =
        catchingAppError(block).getOrElse {
            AppLogger.i(TAG, "No $kind chapters from ${Url(url).host}: ${it::class.simpleName}")
            emptyList()
        }
}

@Serializable
private class ChaptersJson(val chapters: List<JsonChapter> = emptyList())

/** One chapter of the Podcasting 2.0 JSON format; `toc: false` ones are not listed (they only change the art). */
@Serializable
private class JsonChapter(val startTime: Double = 0.0, val title: String? = null, val toc: Boolean = true)
