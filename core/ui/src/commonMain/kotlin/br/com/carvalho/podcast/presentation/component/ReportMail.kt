package br.com.carvalho.podcast.presentation.component

// ponytail: a stand-in address, never shown on screen; replace it with the app's real contact before publishing.
private const val REPORT_EMAIL = "denuncias@example.com"

/** What is reported (26.7): a podcast, or one of its episodes when [episodeTitle] is set. */
data class ReportTarget(val podcastTitle: String, val feedUrl: String, val episodeTitle: String? = null)

fun reportMailto(subject: String, body: String) =
    "mailto:$REPORT_EMAIL?subject=${subject.percentEncoded()}&body=${body.percentEncoded()}"

/** Only the host: no scheme, credentials, path or query, where a private feed keeps its token. */
fun feedHost(feedUrl: String): String =
    feedUrl.substringAfter("://").substringBefore('/').substringBefore('?').substringAfterLast('@')

private const val UNRESERVED = "-._~"
private const val HEX = "0123456789ABCDEF"
private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0x0F
private const val BYTE_MASK = 0xFF
private const val ASCII_END = 0x80

private fun String.percentEncoded(): String = buildString {
    for (byte in this@percentEncoded.encodeToByteArray()) {
        val value = byte.toInt() and BYTE_MASK
        val char = value.toChar()
        if (value < ASCII_END && (char.isLetterOrDigit() || char in UNRESERVED)) {
            append(char)
        } else {
            append('%').append(HEX[value shr NIBBLE_BITS]).append(HEX[value and NIBBLE_MASK])
        }
    }
}
