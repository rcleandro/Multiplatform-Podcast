package br.com.carvalho.podcast.core.util

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

actual object FileUtils {
    /**
     * No WasmJs, o FileSystem do Okio SYSTEM não está disponível por padrão.
     * Usamos um FakeFileSystem (em memória) como placeholder até uma implementação com Cache API.
     */
    actual val fileSystem: FileSystem = FakeFileSystem()

    actual val baseDir: Path = "/".toPath()
}
