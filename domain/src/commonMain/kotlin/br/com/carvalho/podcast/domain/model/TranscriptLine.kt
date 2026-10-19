package br.com.carvalho.podcast.domain.model

/** A stretch of an episode's transcript (19.2): what is said from [startMs] to [endMs], and by whom when known. */
data class TranscriptLine(val startMs: Long, val endMs: Long, val text: String, val speaker: String? = null)

/** The line being said at [positionMs]: the last one that already started. */
fun List<TranscriptLine>.lineAt(positionMs: Long): Int = indexOfLast { it.startMs <= positionMs }
