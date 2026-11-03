package br.com.carvalho.podcast.presentation.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import br.com.carvalho.podcast.core.AppConfig
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch

/**
 * Opens the system's file picker and hands the chosen file's text to [onText], or null when it cannot be read; nothing
 * when the user gives up. Any file can be picked: Android knows no type for `.opml`, so filtering by it would grey out
 * the very file wanted, and the content says whether it fits.
 */
@Composable
fun rememberTextFilePicker(onText: (String?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val launcher = rememberFilePickerLauncher { file ->
        // Too big to be a real OPML or backup (22.3): counts as unreadable rather than filling the memory.
        if (file != null) scope.launch { onText(runCatching { file.readTextUpToLimit() }.getOrNull()) }
    }
    return { launcher.launch() }
}

private suspend fun PlatformFile.readTextUpToLimit(): String? =
    if (size() > AppConfig.MAX_TEXT_BYTES) null else readString()

/**
 * Saves [text] as a file the user places, named [name] with [extension]: the system's "Save as" where there is one, a
 * download on the Web. [onSaved] says whether it was written; nothing when the user gives up.
 */
@Composable
expect fun rememberTextFileSaver(onSaved: (Boolean) -> Unit): (name: String, extension: String, text: String) -> Unit
