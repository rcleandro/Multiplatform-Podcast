package br.com.carvalho.podcast.core.sync

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.encoding.Base64
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class FolderFilesTest {

    @Test
    fun readsOnlyThisAppsFilesAndWritesThroughTheBookmark() = runTest {
        val dir = Files.createTempDirectory("sync")
        dir.resolve("podcast-sync-laptop.json").writeText("{}")
        dir.resolve("notes.txt").writeText("not ours")
        Files.createDirectory(dir.resolve("podcast-sync-folder.json"))
        val bookmark = Base64.encode(PlatformFile(dir.toString()).bookmarkData().bytes)
        val files = FolderFiles(bookmark)

        files.write("podcast-sync-phone.json", "{\"queue\":[]}")

        assertEquals(
            mapOf("podcast-sync-laptop.json" to "{}", "podcast-sync-phone.json" to "{\"queue\":[]}"),
            files.read(),
        )
        assertEquals("{\"queue\":[]}", dir.resolve("podcast-sync-phone.json").readText())
    }
}
