package br.com.carvalho.podcast.core.observability

// Scheme, optional credentials, host, then whatever follows up to a blank or quote.
private val URL = Regex("""([a-zA-Z][a-zA-Z0-9+.-]*://)(?:[^\s/@]*@)?([^\s/?#:@'"<>]+)([^\s'"<>]*)""")

/**
 * Replaces every URL in [text] with its scheme and host. Private feeds carry access tokens in the path, the query or
 * the credentials, so a full URL must never reach logs or crash reports.
 */
fun redactUrls(text: String): String = URL.replace(text) { match ->
    val (scheme, host, rest) = match.destructured
    if (rest.isEmpty()) "$scheme$host" else "$scheme$host/…"
}

/** Lowercase host of [url], or an empty string when it is not a URL. The only part of a feed URL safe to report. */
fun urlHost(url: String): String = URL.find(url)?.groupValues?.get(2)?.lowercase().orEmpty()
