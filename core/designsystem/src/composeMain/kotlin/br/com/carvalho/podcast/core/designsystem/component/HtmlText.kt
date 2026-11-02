package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
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
