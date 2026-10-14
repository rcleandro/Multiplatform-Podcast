package br.com.carvalho.podcast.core.util

actual val supportsDownloads: Boolean = true

actual val canLimitDownloadsToWifi: Boolean = true

actual val canEnhanceSpeech: Boolean = true

actual val openSystemLanguageSettings: (() -> Unit)? = null
