package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

/** Feed descriptions: paragraphs, lists, bold, italics and clickable links. */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedString = remember(html, linkColor) {
        parseHtml(html, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)))
    }
    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = onTextLayout,
    )
}

private val TAG_OR_TEXT =
    Regex("""<!--.*?-->|<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>|[^<]+|<""", RegexOption.DOT_MATCHES_ALL)
private val HREF = Regex("""href\s*=\s*(?:"([^"]*)"|'([^']*)')""", RegexOption.IGNORE_CASE)
private val ENTITY = Regex("""&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);""")
private val WHITESPACE = Regex("""\s+""")
private const val HEX = 16

// The entities feeds use (counted in the 15.2 fixtures), beyond the five XML ones.
private val NAMED_ENTITIES = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00a0",
    "mdash" to "—", "ndash" to "–", "hellip" to "…", "middot" to "·", "bull" to "•", "rarr" to "→", "larr" to "←",
    "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”", "laquo" to "«", "raquo" to "»",
    "copy" to "©", "reg" to "®", "trade" to "™",
)

/**
 * Turns the HTML of a feed description into styled text. `AnnotatedString.fromHtml` exists only on Android in
 * Compose Multiplatform, so this common parser covers what feeds use: `p`, `br`, `div`, `ul`/`ol`/`li`, `b`/`strong`,
 * `i`/`em` and `a href`. Other tags are dropped and keep their text; whitespace collapses as in a browser.
 */
internal fun parseHtml(html: String, linkStyles: TextLinkStyles? = null): AnnotatedString {
    val writer = HtmlWriter(linkStyles)
    for (match in TAG_OR_TEXT.findAll(html)) {
        val (slash, name, attributes) = match.destructured
        when {
            name.isNotEmpty() && slash.isNotEmpty() -> writer.close(name.lowercase())
            name.isNotEmpty() -> writer.open(name.lowercase(), attributes)
            !match.value.startsWith("<!--") -> writer.text(match.value)
        }
    }
    return writer.result()
}

private class HtmlWriter(private val linkStyles: TextLinkStyles?) {
    private val builder = AnnotatedString.Builder()

    // Mirror of the text so far: AnnotatedString.Builder cannot be read back cheaply.
    private val plain = StringBuilder()
    private val openTags = mutableListOf<String>()

    // A space is written only when more text follows, so none is left before a line break.
    private var pendingSpace = false

    fun text(raw: String) {
        val collapsed = decodeEntities(raw.replace(WHITESPACE, " "))
        val words = collapsed.trim(' ')
        if (collapsed.startsWith(' ')) pendingSpace = true
        if (words.isNotEmpty()) {
            write(words)
            pendingSpace = collapsed.endsWith(' ')
        }
    }

    fun open(name: String, attributes: String) {
        when (name) {
            "b", "strong" -> push(name) { builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            "i", "em" -> push(name) { builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            "a" -> HREF.find(attributes)?.destructured?.let { (double, single) -> double.ifEmpty { single } }
                ?.takeIf { it.isNotBlank() }
                ?.let { href -> push(name) { builder.pushLink(LinkAnnotation.Url(decodeEntities(href), linkStyles)) } }
            "br" -> lineBreaks(trailingLineBreaks() + 1)
            "li" -> {
                lineBreaks(1)
                write("• ")
            }
            else -> blockBreak(name)
        }
    }

    fun close(name: String) {
        val index = openTags.lastIndexOf(name)
        if (index != -1) {
            // Closing an outer tag also closes what was left open inside it.
            repeat(openTags.size - index) {
                builder.pop()
                openTags.removeAt(openTags.lastIndex)
            }
        } else {
            blockBreak(name)
        }
    }

    fun result(): AnnotatedString {
        repeat(openTags.size) { builder.pop() }
        val text = builder.toAnnotatedString()
        val end = plain.trimEnd().length
        return if (end == text.length) text else text.subSequence(0, end)
    }

    private fun blockBreak(name: String) {
        when (name) {
            "p", "ul", "ol" -> lineBreaks(2)
            "div" -> lineBreaks(1)
        }
    }

    // The pending space goes before the style starts, so a styled word does not begin with it.
    private fun push(name: String, start: () -> Unit) {
        flushSpace()
        start()
        openTags += name
    }

    private fun write(value: String) {
        flushSpace()
        builder.append(value)
        plain.append(value)
    }

    private fun flushSpace() {
        if (pendingSpace && plain.isNotEmpty() && plain.last() != '\n') {
            builder.append(' ')
            plain.append(' ')
        }
        pendingSpace = false
    }

    private fun lineBreaks(count: Int) {
        pendingSpace = false
        if (plain.isEmpty()) return
        repeat(count - trailingLineBreaks()) {
            builder.append('\n')
            plain.append('\n')
        }
    }

    private fun trailingLineBreaks() = plain.takeLastWhile { it == '\n' }.length
}

private fun decodeEntities(text: String): String = ENTITY.replace(text) { match ->
    val name = match.groupValues[1]
    val code = when {
        name.startsWith("#x", ignoreCase = true) -> name.drop(2).toIntOrNull(HEX)
        name.startsWith("#") -> name.drop(1).toIntOrNull()
        else -> null
    }
    when {
        code != null -> codePointToString(code) ?: match.value
        else -> NAMED_ENTITIES[name] ?: match.value
    }
}

private const val MAX_BMP = 0xFFFF
private const val MAX_CODE_POINT = 0x10FFFF
private const val SUPPLEMENTARY_BASE = 0x10000
private const val HIGH_SURROGATE = 0xD800
private const val LOW_SURROGATE = 0xDC00
private const val SURROGATE_BITS = 10
private const val LOW_MASK = 0x3FF

// Common code has no Character.toChars; emoji (&#128512;) need a surrogate pair.
private fun codePointToString(code: Int): String? = when {
    code < 0 || code > MAX_CODE_POINT -> null
    code <= MAX_BMP -> code.toChar().toString()
    else -> {
        val offset = code - SUPPLEMENTARY_BASE
        charArrayOf(
            (HIGH_SURROGATE + (offset shr SURROGATE_BITS)).toChar(),
            (LOW_SURROGATE + (offset and LOW_MASK)).toChar(),
        ).concatToString()
    }
}
