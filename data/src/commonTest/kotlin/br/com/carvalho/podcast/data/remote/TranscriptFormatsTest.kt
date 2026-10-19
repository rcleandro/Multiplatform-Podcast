package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.domain.model.TranscriptLine
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class TranscriptFormatsTest {

    @Test
    fun webVttSkipsItsHeaderAndNotesAndNamesTheSpeakerOfAVoiceTag() {
        val vtt = """
            WEBVTT
            Kind: captions

            NOTE written by hand

            intro
            00:00:01.000 --> 00:00:04.500 align:start
            <v Alice>Hello and <i>welcome</i>
            to the show.

            01:00:00.250 --> 01:00:02.000
            <v.loud Bob>Bye!
        """.trimIndent()

        assertEquals(
            listOf(
                TranscriptLine(1_000, 4_500, "Hello and welcome to the show.", "Alice"),
                TranscriptLine(3_600_250, 3_602_000, "Bye!", "Bob"),
            ),
            parseCues(vtt),
        )
    }

    @Test
    fun srtUsesCommasAndNumbersEachCue() {
        val srt = "1\r\n00:00:02,500 --> 00:00:03,000\r\nFirst line\r\n\r\n2\r\n00:01:00,000 --> 00:01:01,000\r\nSecond\r\n"

        assertEquals(
            listOf(TranscriptLine(2_500, 3_000, "First line"), TranscriptLine(60_000, 61_000, "Second")),
            parseCues(srt),
        )
    }

    @Test
    fun jsonWordsJoinIntoLinesBySpeakerAndSentence() {
        val transcript = """
            {"version":"1.0.0","segments":[
              {"speaker":"Alice","startTime":0.0,"endTime":0.4,"body":"Hello"},
              {"speaker":"Alice","startTime":0.4,"endTime":0.9,"body":"there."},
              {"speaker":"Alice","startTime":1.0,"endTime":1.5,"body":"Next"},
              {"speaker":"Bob","startTime":2.0,"endTime":2.5,"body":"Hi"},
              {"speaker":"Bob","startTime":2.5,"endTime":2.6,"body":" "}
            ]}
        """.trimIndent()

        assertEquals(
            listOf(
                TranscriptLine(0, 900, "Hello there.", "Alice"),
                TranscriptLine(1_000, 1_500, "Next", "Alice"),
                TranscriptLine(2_000, 2_500, "Hi", "Bob"),
            ),
            parseJsonTranscript(transcript, Json { ignoreUnknownKeys = true }),
        )
    }
}
