package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import kotlin.test.Test
import kotlin.test.assertEquals

class KnownDurationTest {
    private val episode = Episode(
        id = "e", podcastId = "p", title = "e", description = null, audioUrl = "", imageUrl = null,
        duration = FEED_SECONDS, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false,
        fileSize = null,
    )

    @Test
    fun theFeedsLengthServesUntilTheAudioLoads() {
        assertEquals(FEED_SECONDS * 1_000, PlayerState(currentEpisode = episode).knownDurationMs())
        assertEquals(FEED_SECONDS * 1_000, PlayerState(currentEpisode = episode, duration = 0).knownDurationMs())
    }

    @Test
    fun theAudiosOwnLengthWinsOnceKnown() {
        assertEquals(LOADED_MS, PlayerState(currentEpisode = episode, duration = LOADED_MS).knownDurationMs())
        assertEquals(0, PlayerState().knownDurationMs())
    }

    private companion object {
        const val FEED_SECONDS = 1_440L
        const val LOADED_MS = 1_445_000L
    }
}
