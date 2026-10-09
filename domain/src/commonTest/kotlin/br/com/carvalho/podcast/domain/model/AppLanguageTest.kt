package br.com.carvalho.podcast.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun aTagIsReadByItsLanguageWhateverTheRegion() {
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.of("pt-BR"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.of("ES"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.of("en"))
    }

    @Test
    fun noTagOrOneTheAppDoesNotSpeakFollowsTheSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.of(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.of(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.of("fr-FR"))
    }
}
