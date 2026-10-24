package br.com.carvalho.podcast.core.image

import coil3.PlatformContext
import okio.Path

// The browser has no file system for the page; its own HTTP cache keeps the covers.
internal actual fun imageCacheDirectory(context: PlatformContext): Path? = null
