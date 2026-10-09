package br.com.carvalho.podcast.core.util

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => navigator.language")
private external fun browserLanguage(): String

/** The region of the browser's language ("pt-BR" → "BR"); a language alone ("pt") has none. */
actual fun deviceCountry(): String? = browserLanguage().substringAfter('-', "").uppercase().ifBlank { null }
