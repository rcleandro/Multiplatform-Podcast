package br.com.carvalho.podcast.presentation.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import kotlinx.coroutines.launch

/** A browser has no "Save as" a page can open: the file is downloaded, and the browser asks where if set to. */
@Composable
actual fun rememberTextFileSaver(onSaved: (Boolean) -> Unit): (name: String, extension: String, text: String) -> Unit {
    val scope = rememberCoroutineScope()
    return { name, extension, text ->
        scope.launch {
            onSaved(runCatching { FileKit.download(text.encodeToByteArray(), "$name.$extension") }.isSuccess)
        }
    }
}
