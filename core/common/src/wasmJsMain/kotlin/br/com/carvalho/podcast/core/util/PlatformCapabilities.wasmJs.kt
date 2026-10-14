package br.com.carvalho.podcast.core.util

// ponytail: the Web keeps files in memory (FakeFileSystem), lost on reload; turn on with persistent storage (OPFS).
actual val supportsDownloads: Boolean = false

actual val canLimitDownloadsToWifi: Boolean = false

actual val canEnhanceSpeech: Boolean = false

actual val canRefreshInBackground: Boolean = false

actual val openSystemLanguageSettings: (() -> Unit)? = null
