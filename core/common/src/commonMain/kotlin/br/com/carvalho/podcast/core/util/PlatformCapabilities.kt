package br.com.carvalho.podcast.core.util

/** Whether downloaded episodes survive on this platform; the screens hide downloads where they would not. */
expect val supportsDownloads: Boolean

/** Whether downloads can wait for Wi-Fi: phones and tablets have mobile data; computers and the Web do not. */
expect val canLimitDownloadsToWifi: Boolean
