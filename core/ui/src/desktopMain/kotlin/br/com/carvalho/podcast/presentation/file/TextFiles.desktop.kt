package br.com.carvalho.podcast.presentation.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.launch

@Composable
actual fun rememberTextFileSaver(onSaved: (Boolean) -> Unit): (name: String, extension: String, text: String) -> Unit {
    val scope = rememberCoroutineScope()
    // The dialog only gives the place; the text waits here until it does.
    var pending by remember { mutableStateOf<String?>(null) }
    val launcher = rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) { file ->
        val text = pending
        pending = null
        if (file != null && text != null) scope.launch { onSaved(runCatching { file.writeString(text) }.isSuccess) }
    }
    return { name, extension, text ->
        pending = text
        launcher.launch(suggestedName = name, defaultExtension = extension)
    }
}
