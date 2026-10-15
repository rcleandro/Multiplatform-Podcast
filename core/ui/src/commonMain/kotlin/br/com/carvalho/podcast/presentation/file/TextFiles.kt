package br.com.carvalho.podcast.presentation.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readString
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
        if (file != null) scope.launch { onText(runCatching { file.readString() }.getOrNull()) }
    }
    return { launcher.launch() }
}

/**
 * Saves [text] as a file the user places, named [name] with [extension]: the system's "Save as" where there is one, a
 * download on the Web. [onSaved] says whether it was written; nothing when the user gives up.
 */
@Composable
expect fun rememberTextFileSaver(onSaved: (Boolean) -> Unit): (name: String, extension: String, text: String) -> Unit
