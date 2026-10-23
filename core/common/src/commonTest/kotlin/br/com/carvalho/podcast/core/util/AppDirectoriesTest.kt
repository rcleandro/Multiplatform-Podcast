package br.com.carvalho.podcast.core.util

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDirectoriesTest {
    // The Android backup rules (20.1) leave out exactly this folder: "downloads/" under the app's files.
    @Test
    fun downloadsLiveInTheirOwnFolderUnderTheAppsFiles() {
        val directories = AppDirectories(FakeFileSystem(), "/app/files".toPath())

        assertEquals("/app/files/downloads".toPath(), directories.downloadsDir)
        assertEquals("/app/files/downloads/3f2a.mp3".toPath(), directories.downloadPath("3f2a.mp3"))
    }
}
