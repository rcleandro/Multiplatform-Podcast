package br.com.carvalho.podcast.core.util

// The Apple Watch (ADR 0016): it downloads for listening away from the phone, and refreshes when it opens or syncs.

actual val supportsDownloads: Boolean = true

actual val canLimitDownloadsToWifi: Boolean = true

actual val canEnhanceSpeech: Boolean = false

actual val canRefreshInBackground: Boolean = false

/** The watch has no per-app language setting to open. */
actual val openSystemLanguageSettings: (() -> Unit)? = null
