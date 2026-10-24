package br.com.carvalho.podcast.core.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

// Library/Caches: kept between runs, out of the backup, cleared by iOS only when the device runs short of space.
internal actual fun imageCacheDirectory(context: PlatformContext): Path? =
    (NSFileManager.defaultManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask).first() as NSURL)
        .path!!.toPath() / "image_cache"
