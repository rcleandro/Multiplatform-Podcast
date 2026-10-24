package br.com.carvalho.podcast.core.image

import coil3.PlatformContext
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageCacheDirectoryTest {
    // Not the system's temporary folder, which Linux empties at boot (23.7): beside the app's own data.
    @Test
    fun coversAreKeptBesideTheAppsData() {
        val home = System.getProperty("user.home").toPath()

        assertEquals(home / ".podcast" / "cache" / "images", imageCacheDirectory(PlatformContext.INSTANCE))
    }
}
