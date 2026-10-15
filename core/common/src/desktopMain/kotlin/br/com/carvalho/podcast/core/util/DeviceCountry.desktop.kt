package br.com.carvalho.podcast.core.util

import java.util.Locale

actual fun deviceCountry(): String? = Locale.getDefault().country.ifBlank { null }
