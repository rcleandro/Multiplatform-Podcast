package br.com.carvalho.podcast.domain.player

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.TranscriptLine

/** Reads the transcript an episode's feed points to (19.2), in time order. */
fun interface TranscriptSource {
    suspend fun transcript(episode: Episode): Result<List<TranscriptLine>>
}
