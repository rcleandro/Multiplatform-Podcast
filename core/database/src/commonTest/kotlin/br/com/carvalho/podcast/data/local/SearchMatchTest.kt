package br.com.carvalho.podcast.data.local

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchMatchTest {
    @Test
    fun eachWordTypedBecomesARequiredPrefix() {
        assertEquals("inteligencia* artifi*", searchMatch("  inteligencia   artifi "))
    }

    @Test
    fun whatTheSearchSyntaxWouldReadIsLeftOut() {
        // Quotes, stars, dashes and columns are full-text syntax; typed, they would break or widen the query.
        assertEquals("ação* c* kotlin* title*", searchMatch("\"ação\" c++ -kotlin* title:"))
        assertEquals("", searchMatch(" \"*\" - "))
        assertEquals("", searchMatch(""))
    }
}
