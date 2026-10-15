package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.TranscriptLine
import br.com.carvalho.podcast.domain.player.TranscriptSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Downloads the transcript the feed gives (19.2) each time it is opened, and reads it by its type. */
// ponytail: not cached, so a transcript needs the network; save it with the download if offline reading is asked for.
class EpisodeTranscriptSource(
    private val client: HttpClient,
    private val json: Json,
    private val dispatchers: CoroutineDispatchers,
) : TranscriptSource {
    override suspend fun transcript(episode: Episode): Result<List<TranscriptLine>> = catchingAppError {
        val url = episode.transcriptUrl ?: return@catchingAppError emptyList()
        val response = client.get(url)
        if (!response.status.isSuccess()) throw AppError.Http(response.status.value)
        val text = response.bodyAsText()
        withContext(dispatchers.default) {
            if ("json" in episode.transcriptType.orEmpty()) parseJsonTranscript(text, json) else parseCues(text)
        }
    }
}
