package br.com.carvalho.podcast.presentation

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.domain.model.AppLanguage

@Composable
internal actual fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) = content()
