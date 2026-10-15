package br.com.carvalho.podcast.feature.podcast.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class SeasonHeadersTest {
    @Test
    fun aHeaderStartsEachSeasonAndAnEpisodeWithoutOneDoesNotBreakIt() {
        val seasons = listOf(2, 2, null, 2, 1, 1)

        val headers = seasons.indices.map { index -> seasonStartingAt(index) { seasons[it] } }

        assertEquals(listOf(2, null, null, null, 1, null), headers)
    }

    @Test
    fun aShowWithoutSeasonsHasNoHeaders() {
        assertEquals(null, seasonStartingAt(0) { null })
    }
}
