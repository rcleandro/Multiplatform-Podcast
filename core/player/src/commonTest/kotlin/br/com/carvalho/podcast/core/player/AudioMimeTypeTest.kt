package br.com.carvalho.podcast.core.player

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioMimeTypeTest {
    @Test
    fun theAddressExtensionNamesTheAudioTypeAndMp3IsTheRest() {
        assertEquals("audio/mp4", audioMimeType("https://cdn.example.com/ep/42.M4A?token=abc.mp3"))
        assertEquals("audio/ogg", audioMimeType("https://cdn.example.com/ep.opus"))
        assertEquals("audio/mpeg", audioMimeType("https://cdn.example.com/ep.mp3"))
        assertEquals("audio/mpeg", audioMimeType("https://feeds.example.com/play/12345"))
    }
}
