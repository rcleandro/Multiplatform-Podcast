package br.com.carvalho.podcast.core.designsystem

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import br.com.carvalho.podcast.core.designsystem.component.parseHtml
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlParserTest {
    @Test
    fun keepsTextAndDropsTags() {
        assertEquals("Olá mundo", parseHtml("<p>Olá <b>mundo</b></p>").text)
    }

    @Test
    fun boldTextKeepsItsStyle() {
        val text = parseHtml("Olá <strong>mundo</strong>")

        val bold = text.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
        assertEquals("mundo", text.text.substring(bold.start, bold.end))
    }

    @Test
    fun decodesEntities() {
        assertEquals(
            "Rock & Roll – parte 1 — já vem… “isso” ’",
            parseHtml("Rock &amp; Roll &#8211; parte 1 &mdash; já&nbsp;vem&hellip; &ldquo;isso&rdquo; &#x2019;").text,
        )
        assertEquals("&unknown; stays", parseHtml("&unknown; stays").text)
        assertEquals("\uD83D\uDE00 2 < 3", parseHtml("&#128512; 2 &lt; 3").text)
    }

    @Test
    fun sourceLineBreaksAreSpacesAndBlocksAreParagraphs() {
        assertEquals(
            "Linha um continua\n\nDois\nTrês",
            parseHtml("\n<p>Linha um\n   continua</p>\n\n<p>Dois<br/>Três</p>\n").text,
        )
    }

    @Test
    fun listsBecomeBullets() {
        assertEquals("Links:\n\n• A\n• B", parseHtml("<p>Links:</p><ul>\n<li>A</li>\n<li>B</li>\n</ul>").text)
    }

    @Test
    fun unknownTagsKeepTheirText() {
        assertEquals("um\ndois três", parseHtml("<div>um</div><div>dois <span class=\"x\">três</span></div>").text)
    }

    @Test
    fun linksAreClickable() {
        val text = parseHtml("Veja <a href=\"https://example.com/a?b=1&amp;c=2\" target=\"_blank\">o site</a>.")

        assertEquals("Veja o site.", text.text)
        val link = text.getLinkAnnotations(0, text.length).single()
        assertEquals("https://example.com/a?b=1&c=2", (link.item as LinkAnnotation.Url).url)
        assertEquals("o site", text.text.substring(link.start, link.end))
    }

    @Test
    fun readsADescriptionShapedLikeARealOne() {
        // Shaped like the Hipsters Ponto Tech descriptions (data/src/commonTest/resources/feeds/hipsters.xml).
        val html = "<p>Neste episódio, conversamos sobre <strong>liderança</strong> &#8211; com " +
            "<a href=\"https://www.linkedin.com/in/example/\">Fulana</a>.</p>\n<p>Convidados:</p>\n" +
            "<ul>\n<li>Paulo Silveira &mdash; host</li>\n</ul>"

        assertEquals(
            "Neste episódio, conversamos sobre liderança – com Fulana.\n\nConvidados:\n\n• Paulo Silveira — host",
            parseHtml(html).text,
        )
    }
}
