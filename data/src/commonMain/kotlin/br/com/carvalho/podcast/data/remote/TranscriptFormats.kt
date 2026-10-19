package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.domain.model.TranscriptLine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val MILLIS_PER_SECOND = 1000.0
private const val SECONDS_PER_MINUTE = 60
private const val ARROW = "-->"
private val TAG = Regex("<[^>]*>")
private val VOICE = Regex("<v(?:\\.[^ >]*)?\\s+([^>]+)>")
private val SENTENCE_END = Regex("[.!?…。？！]$")

/** Above this a JSON transcript's line ends even mid-sentence, so following the audio never lags far behind. */
private const val MAX_LINE_MS = 15_000L

/**
 * WebVTT and SRT (19.2): blocks split by blank lines, each with a "start --> end" line and its text. Anything else
 * (the WebVTT header, NOTE and STYLE blocks, cue numbers) is skipped. A WebVTT voice tag (`<v Name>`) names the
 * speaker; other tags are dropped.
 */
internal fun parseCues(text: String): List<TranscriptLine> =
    text.replace("\r\n", "\n").replace('\r', '\n').split(Regex("\n\\s*\n")).mapNotNull { block ->
        val lines = block.lines()
        val timing = lines.indexOfFirst { ARROW in it }.takeIf { it >= 0 } ?: return@mapNotNull null
        val (start, end) = lines[timing].split(ARROW).map { it.trim().substringBefore(' ') }
        val body = lines.drop(timing + 1).joinToString(" ").trim()
        val speaker = VOICE.find(body)?.groupValues?.get(1)?.trim()
        val said = body.replace(TAG, "").replace(Regex("\\s+"), " ").trim()
        val startMs = timestamp(start) ?: return@mapNotNull null
        said.ifEmpty { null }?.let { TranscriptLine(startMs, timestamp(end) ?: startMs, it, speaker) }
    }.sortedBy { it.startMs }

/** "01:02:03.456", "02:03,456" or "3.5" as milliseconds; null when it is not a time. */
private fun timestamp(value: String): Long? {
    val parts = value.replace(',', '.').split(':')
    val seconds = parts.last().toDoubleOrNull()
    val units = parts.dropLast(1).map { it.toLongOrNull() }
    if (seconds == null || null in units) return null
    val whole = units.filterNotNull().fold(0L) { total, part -> total * SECONDS_PER_MINUTE + part }
    return ((whole * SECONDS_PER_MINUTE + seconds) * MILLIS_PER_SECOND).toLong()
}

/**
 * The Podcasting 2.0 JSON transcript (19.2). Its segments are often single words, so they join into lines: a new one
 * for a new speaker, after a sentence ends, or when the line gets long.
 */
internal fun parseJsonTranscript(text: String, json: Json): List<TranscriptLine> {
    val segments = json.decodeFromString<JsonTranscript>(text).segments
        .filter { it.body.isNotBlank() }
        .sortedBy { it.startTime }
    val lines = mutableListOf<TranscriptLine>()
    for (segment in segments) {
        val start = (segment.startTime * MILLIS_PER_SECOND).toLong()
        val end = (segment.endTime * MILLIS_PER_SECOND).toLong()
        val last = lines.lastOrNull()
        val joins = last != null && last.speaker == segment.speaker && !SENTENCE_END.containsMatchIn(last.text) &&
            start - last.startMs < MAX_LINE_MS
        if (joins) {
            lines[lines.lastIndex] = last.copy(endMs = end, text = "${last.text} ${segment.body.trim()}")
        } else {
            lines += TranscriptLine(start, end, segment.body.trim(), segment.speaker)
        }
    }
    return lines
}

@Serializable
private class JsonTranscript(val segments: List<JsonSegment> = emptyList())

@Serializable
private class JsonSegment(
    val startTime: Double = 0.0,
    val endTime: Double = 0.0,
    val body: String = "",
    val speaker: String? = null,
)
