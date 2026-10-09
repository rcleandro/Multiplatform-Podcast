package br.com.carvalho.podcast.core.util

actual val supportsDownloads: Boolean = true

actual val canLimitDownloadsToWifi: Boolean = false

actual val openSystemLanguageSettings: (() -> Unit)? = null
