package br.com.carvalho.podcast.core.image

import coil3.PlatformContext
import kotlin.test.Test
import kotlin.test.assertTrue

class ImageCacheDirectoryTest {
    // Not tmp/, which iOS empties while the app is closed (23.7): Library/Caches, kept until space runs short.
    @Test
    fun coversAreKeptInLibraryCaches() {
        val folder = imageCacheDirectory(PlatformContext.INSTANCE).toString()

        assertTrue(folder.endsWith("/Library/Caches/image_cache"), folder)
    }
}
