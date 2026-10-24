package br.com.carvalho.podcast.data.local

/**
 * What the user typed as a full-text query (23.4): every word a required prefix, so "inteligencia artifi" finds
 * "Inteligência artificial". Anything but letters and digits splits words, which keeps quotes, `*`, `-` and `:` from
 * reaching the query syntax. Empty when nothing searchable was typed.
 */
fun searchMatch(query: String): String = query
    .map { if (it.isLetterOrDigit()) it else ' ' }
    .joinToString("")
    .split(' ')
    .filter { it.isNotEmpty() }
    .joinToString(" ") { "$it*" }
