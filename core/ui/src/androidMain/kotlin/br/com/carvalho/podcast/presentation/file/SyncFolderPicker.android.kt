package br.com.carvalho.podcast.presentation.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64

@Composable
actual fun rememberSyncFolderPicker(onPicked: (bookmark: String, name: String) -> Unit): (() -> Unit)? {
    val scope = rememberCoroutineScope()
    val launcher = rememberDirectoryPickerLauncher { folder ->
        if (folder != null) {
            scope.launch {
                val bookmark = runCatching { folder.bookmarkData() }.getOrNull() ?: return@launch
                onPicked(Base64.encode(bookmark.bytes), folder.name)
            }
        }
    }
    return { launcher.launch() }
}
