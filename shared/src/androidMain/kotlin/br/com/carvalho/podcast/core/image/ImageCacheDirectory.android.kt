package br.com.carvalho.podcast.core.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

internal actual fun imageCacheDirectory(context: PlatformContext): Path? = context.cacheDir.toOkioPath() / "image_cache"
