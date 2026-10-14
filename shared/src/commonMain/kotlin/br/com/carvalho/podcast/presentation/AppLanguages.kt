package br.com.carvalho.podcast.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.LayoutDirection
import br.com.carvalho.podcast.domain.model.AppLanguage

/**
 * Speaks [language] in [content] (18.5). Android applies it to the whole app through its per-app language
 * (`MainActivity`), so notifications follow too; iOS leaves the choice to its Settings; Desktop and the Web switch
 * Compose's locale here.
 */
@Composable
internal expect fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit)

/** Languages written right to left; of those the app speaks, Arabic (18.20). */
private val RIGHT_TO_LEFT = setOf("ar", "fa", "he", "iw", "ur")

/**
 * Lays [content] out right to left when the language in use reads that way (18.20). Android already does it from its
 * locale; Desktop and the Web keep left to right whatever the locale, so the root follows it here.
 */
@Composable
internal fun ProvideLayoutDirection(content: @Composable () -> Unit) {
    val direction = if (Locale.current.language in RIGHT_TO_LEFT) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction, content = content)
}
