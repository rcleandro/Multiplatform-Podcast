package br.com.carvalho.podcast.wear

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeLeftTest {
    private val episode = Episode(
        id = "1",
        podcastId = "p",
        title = "Episode",
        description = null,
        audioUrl = "https://example.com/1.mp3",
        imageUrl = null,
        duration = 1_800,
        publishDate = 0,
        isPlayed = false,
        playbackPosition = 600_000,
        isDownloaded = false,
        fileSize = null,
    )

    @Test
    fun `what is left is the length less the position`() {
        assertEquals("20min", timeLeft(PlayerState(position = 600_000, duration = 1_800_000)))
    }

    @Test
    fun `the last seconds still read as a minute`() {
        assertEquals("1min", timeLeft(PlayerState(position = 1_790_000, duration = 1_800_000)))
    }

    @Test
    fun `before the audio loads the feed's length stands in`() {
        assertEquals("20min", timeLeft(PlayerState(currentEpisode = episode, position = 600_000)))
    }

    @Test
    fun `an unknown length shows nothing`() {
        assertNull(timeLeft(PlayerState(position = 600_000, duration = null)))
        assertNull(timeLeft(PlayerState(position = 0, duration = 0)))
    }
}
