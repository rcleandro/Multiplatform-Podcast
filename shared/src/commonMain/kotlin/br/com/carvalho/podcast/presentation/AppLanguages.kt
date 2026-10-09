package br.com.carvalho.podcast.presentation

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.domain.model.AppLanguage

/**
 * Speaks [language] in [content] (18.5). Android applies it to the whole app through its per-app language
 * (`MainActivity`), so notifications follow too; iOS leaves the choice to its Settings; Desktop and the Web switch
 * Compose's locale here.
 */
@Composable
internal expect fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit)
