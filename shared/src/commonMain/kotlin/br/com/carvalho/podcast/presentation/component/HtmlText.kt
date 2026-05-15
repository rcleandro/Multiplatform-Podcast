package br.com.carvalho.podcast.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = parseHtml(html)
    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium
    )
}

/**
 * Um parser simples para tags HTML básicas: <b>, <i>, <br>, <p>
 * Para um suporte completo, seria necessária uma biblioteca como Ksoup ou implementação platform-specific.
 */
fun parseHtml(html: String): AnnotatedString {
    return buildAnnotatedString {
        val tagRegex = Regex("<[^>]+>")
        val matches = tagRegex.findAll(html)

        var lastEnd = 0
        val styleStack = mutableListOf<String>()

        for (match in matches) {
            append(html.substring(lastEnd, match.range.first))

            val tag = match.value.lowercase()
            when (tag) {
                "<b>", "<strong>" -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    styleStack.add("b")
                }
                "</b>", "</strong>" -> {
                    if (styleStack.lastOrNull() == "b") {
                        pop()
                        styleStack.removeAt(styleStack.size - 1)
                    }
                }
                "<i>", "<em>" -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    styleStack.add("i")
                }
                "</i>", "</em>" -> {
                    if (styleStack.lastOrNull() == "i") {
                        pop()
                        styleStack.removeAt(styleStack.size - 1)
                    }
                }
                "<br>", "<br/>", "<br />" -> append("\n")
                "<p>" -> {
                    if (length > 0) append("\n\n")
                }
                "</p>" -> append("\n")
            }
            lastEnd = match.range.last + 1
        }
        append(html.substring(lastEnd))

        repeat(styleStack.size) { pop() }
    }
}
