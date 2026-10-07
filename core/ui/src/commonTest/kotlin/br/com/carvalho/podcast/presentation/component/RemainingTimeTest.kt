package br.com.carvalho.podcast.presentation.component

import br.com.carvalho.podcast.domain.model.Episode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemainingTimeTest {
    private fun episode(durationSec: Long, positionMs: Long, played: Boolean = false) = Episode(
        id = "e", podcastId = "p", title = "E", description = null, audioUrl = "a", imageUrl = null,
        duration = durationSec, publishDate = 0, isPlayed = played, playbackPosition = positionMs,
        isDownloaded = false, fileSize = null,
    )

    @Test
    fun aStartedEpisodeSaysWhatIsLeftLikeItsLength() {
        // 2h 52min long, 30 min in: the same "1h 5min" style as the total.
        assertEquals("2h 22min", episode(durationSec = 10_320, positionMs = 1_800_000).remainingDuration())
        assertEquals("1min", episode(durationSec = 600, positionMs = 599_000).remainingDuration())
    }

    @Test
    fun notStartedOrFinishedSaysNothingLeft() {
        assertNull(episode(durationSec = 600, positionMs = 0).remainingDuration())
        assertNull(episode(durationSec = 600, positionMs = 100_000, played = true).remainingDuration())
        assertNull(episode(durationSec = 0, positionMs = 100_000).remainingDuration())
    }
}
