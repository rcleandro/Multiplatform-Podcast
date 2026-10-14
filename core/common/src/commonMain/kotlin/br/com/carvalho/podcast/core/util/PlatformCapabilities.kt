package br.com.carvalho.podcast.core.util

/** Whether downloaded episodes survive on this platform; the screens hide downloads where they would not. */
expect val supportsDownloads: Boolean

/** Whether downloads can wait for Wi-Fi: phones and tablets have mobile data; computers and the Web do not. */
expect val canLimitDownloadsToWifi: Boolean

/**
 * Whether the player can skip silences and lift voices (18.16): Media3 does both on Android; AVPlayer, JavaFX and the
 * browser's audio element offer neither, so the switches show only there.
 */
expect val canEnhanceSpeech: Boolean

/**
 * Whether the app looks for new episodes while closed (18.18): WorkManager on Android. iOS gets it with phase 25;
 * Desktop and the Web refresh when they open.
 */
expect val canRefreshInBackground: Boolean

/**
 * Opens the system's language settings for this app, where the app cannot choose its language itself (iOS keeps
 * that choice in its Settings); null where the app's Settings choose it.
 */
expect val openSystemLanguageSettings: (() -> Unit)?
