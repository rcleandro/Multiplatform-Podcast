package br.com.carvalho.podcast.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun aTagIsReadByItsLanguageWhateverTheRegion() {
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.of("pt-BR"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.of("ES"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.of("en"))
        assertEquals(AppLanguage.CHINESE, AppLanguage.of("zh-Hans-CN"))
        // Android's old code for Indonesian.
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.of("in-ID"))
    }

    @Test
    fun noTagOrOneTheAppDoesNotSpeakFollowsTheSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.of(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.of(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.of("nl-NL"))
    }
}
