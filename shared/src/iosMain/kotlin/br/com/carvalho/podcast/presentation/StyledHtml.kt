package br.com.carvalho.podcast.presentation

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import br.com.carvalho.podcast.core.designsystem.component.parseHtml

/**
 * A feed's HTML description for SwiftUI: its text and the stretches that are bold, italic or a link, read by the same
 * parser as the Compose screens. It keeps only what feeds use and only `https`, `http` and `mailto` links (22.4), where
 * the system's HTML import would run WebKit and could fetch whatever the feed points at. Positions are UTF-16 offsets,
 * as `NSString` counts them.
 */
class StyledText(val text: String, val spans: List<StyledSpan>)

class StyledSpan(val start: Int, val end: Int, val bold: Boolean, val italic: Boolean, val url: String?)

fun styledHtml(html: String): StyledText {
    val parsed = parseHtml(html)
    val styles = parsed.spanStyles.map {
        StyledSpan(
            start = it.start,
            end = it.end,
            bold = it.item.fontWeight == FontWeight.Bold,
            italic = it.item.fontStyle == FontStyle.Italic,
            url = null,
        )
    }
    val links = parsed.getLinkAnnotations(0, parsed.length).mapNotNull { range ->
        val url = (range.item as? LinkAnnotation.Url)?.url ?: return@mapNotNull null
        StyledSpan(range.start, range.end, bold = false, italic = false, url = url)
    }
    return StyledText(parsed.text, styles + links)
}
