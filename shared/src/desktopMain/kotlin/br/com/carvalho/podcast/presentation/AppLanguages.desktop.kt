package br.com.carvalho.podcast.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import br.com.carvalho.podcast.domain.model.AppLanguage
import java.util.Locale

/** The computer's own language, taken before the app changes it. */
private val systemLocale: Locale = Locale.getDefault()

@Composable
internal actual fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    Locale.setDefault(language.tag?.let(Locale::forLanguageTag) ?: systemLocale)
    // Compose resources read the locale while composing: compose everything again in the new one.
    key(language) { content() }
}
