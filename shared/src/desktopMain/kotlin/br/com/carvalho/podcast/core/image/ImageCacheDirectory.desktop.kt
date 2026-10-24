package br.com.carvalho.podcast.core.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath

// Beside the app's database in ~/.podcast (PlatformModule.desktop), where no system clean-up reaches.
internal actual fun imageCacheDirectory(context: PlatformContext): Path? =
    System.getProperty("user.home").toPath() / ".podcast" / "cache" / "images"
