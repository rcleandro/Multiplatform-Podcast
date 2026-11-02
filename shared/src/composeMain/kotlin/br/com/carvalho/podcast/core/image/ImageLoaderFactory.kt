package br.com.carvalho.podcast.core.image

import br.com.carvalho.podcast.core.designsystem.Motion
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import okio.Path

private const val MEMORY_CACHE_SIZE_PERCENT = 0.25
private const val DISK_CACHE_MAX_SIZE = 512L * 1024L * 1024L

/**
 * Where the covers are kept between runs: a folder the system may clear when space runs short, never one it empties on
 * its own; null where the platform keeps its own (the browser's HTTP cache).
 */
internal expect fun imageCacheDirectory(context: PlatformContext): Path?

fun createImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory())
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, MEMORY_CACHE_SIZE_PERCENT)
                .build()
        }
        // In each platform's cache folder (23.7): the system's temporary one is emptied by iOS while the app is closed
        // and by Linux at every boot, so the covers came down again.
        .diskCache {
            imageCacheDirectory(context)?.let { folder ->
                DiskCache.Builder()
                    .directory(folder)
                    .maxSizeBytes(DISK_CACHE_MAX_SIZE)
                    .build()
            }
        }
        .crossfade(Motion.MEDIUM)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
}
