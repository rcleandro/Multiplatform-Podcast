package br.com.carvalho.podcast.domain.usecase

private val SCHEMES = setOf("http", "https")

// Characters RFC 3986 allows in a URL; anything else (a space, a `|` pasted from a list) must be percent-encoded.
private val URL_CHARS = Regex("""[A-Za-z0-9\-._~:/?#\[\]@!$&'()*+,;=%]+""")
private val HOST = Regex("""[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+""")

/**
 * [input] as a feed address, or `null` when it cannot be one: http(s) only, a host with a dot, no stray characters.
 * A missing scheme becomes `https://`. Nothing else changes; comparing addresses is a separate concern.
 */
fun validFeedUrl(input: String): String? {
    val trimmed = input.trim()
    val url = if ("://" in trimmed) trimmed else "https://$trimmed"
    val scheme = url.substringBefore("://")
    val authority = url.substringAfter("://").takeWhile { it !in "/?#" }
    val hostAndPort = authority.substringAfterLast('@')
    val host = hostAndPort.substringBefore(':')
    val port = hostAndPort.substringAfter(':', missingDelimiterValue = "")
    val valid = scheme.lowercase() in SCHEMES && URL_CHARS.matches(url) && HOST.matches(host) &&
        port.all { it.isDigit() }
    return url.takeIf { valid }
}

/**
 * What two addresses of the same feed have in common: no scheme (http and https serve the same feed), lowercase host
 * without `www.`, no fragment and no trailing slash. Path and query keep their case: servers may tell them apart.
 */
fun feedUrlKey(url: String): String {
    val withoutScheme = url.trim().substringAfter("://").substringBefore('#')
    val authority = withoutScheme.takeWhile { it !in "/?" }
    val path = withoutScheme.drop(authority.length).substringBefore('?').trimEnd('/')
    val query = withoutScheme.substringAfter('?', missingDelimiterValue = "")
    val host = authority.lowercase().removePrefix("www.")
    return host + path + if (query.isEmpty()) "" else "?$query"
}
