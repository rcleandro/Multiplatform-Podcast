package br.com.carvalho.podcast.core.util

/** Whether downloaded episodes survive on this platform; the screens hide downloads where they would not. */
expect val supportsDownloads: Boolean

/** Whether downloads can wait for Wi-Fi: phones and tablets have mobile data; computers and the Web do not. */
expect val canLimitDownloadsToWifi: Boolean

/**
 * Opens the system's language settings for this app, where the app cannot choose its language itself (iOS keeps
 * that choice in its Settings); null where the app's Settings choose it.
 */
expect val openSystemLanguageSettings: (() -> Unit)?
