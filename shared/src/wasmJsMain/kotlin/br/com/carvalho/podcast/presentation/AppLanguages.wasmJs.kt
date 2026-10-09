package br.com.carvalho.podcast.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import br.com.carvalho.podcast.domain.model.AppLanguage

/** index.html makes `navigator.languages` answer with this, when set, which is where Compose reads the locale. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(tag) => { window.__customLocale = tag; }")
private external fun setCustomLocale(tag: String?)

@Composable
internal actual fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    setCustomLocale(language.tag)
    key(language) { content() }
}
