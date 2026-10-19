package br.com.carvalho.podcast.core.player

/**
 * The audio type an episode's address suggests, for a Chromecast (18.17): episodes saved before version 27 have no
 * enclosure type, and MP3 is what most podcasts serve.
 */
internal fun audioMimeType(url: String): String = when (url.substringBefore('?').substringAfterLast('.').lowercase()) {
    "m4a", "mp4", "aac" -> "audio/mp4"
    "ogg", "oga", "opus" -> "audio/ogg"
    else -> "audio/mpeg"
}
