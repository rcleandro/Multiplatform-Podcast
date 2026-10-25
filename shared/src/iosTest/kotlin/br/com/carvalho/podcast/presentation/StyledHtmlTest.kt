package br.com.carvalho.podcast.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StyledHtmlTest {
    // A description reaches SwiftUI as text plus where it is bold and where it links, and a link to a local file stays
    // plain text (22.4).
    @Test
    fun aDescriptionKeepsItsBoldAndItsWebLinksOnly() {
        val styled = styledHtml("<p>Hosted by <b>Ana</b>. <a href=\"https://ex.com\">Site</a> <a href=\"file:///etc\">x</a></p>")

        assertEquals("Hosted by Ana. Site x", styled.text)
        val bold = styled.spans.single { it.bold }
        assertEquals("Ana", styled.text.substring(bold.start, bold.end))
        val link = styled.spans.single { it.url != null }
        assertEquals("https://ex.com", link.url)
        assertEquals("Site", styled.text.substring(link.start, link.end))
        assertTrue(styled.spans.none { it.url?.startsWith("file") == true })
    }
}
