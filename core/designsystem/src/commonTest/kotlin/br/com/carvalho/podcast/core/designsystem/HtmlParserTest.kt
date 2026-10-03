package br.com.carvalho.podcast.core.designsystem

import br.com.carvalho.podcast.core.designsystem.component.parseHtml
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlParserTest {
    @Test
    fun keepsTextAndDropsTags() {
        assertEquals("Olá mundo", parseHtml("<p>Olá <b>mundo</b></p>").text.trim())
    }
}
