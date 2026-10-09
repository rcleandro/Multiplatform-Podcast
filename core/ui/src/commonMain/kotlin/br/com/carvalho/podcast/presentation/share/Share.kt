package br.com.carvalho.podcast.presentation.share

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.link_copied
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** The system's share sheet; `null` where there is none (Desktop, Web). */
@Composable
internal expect fun rememberShareSheet(): ((String) -> Unit)?

/**
 * Shares a text (18.4) through the system's sheet on phones, or copies it where there is none and says so in
 * [snackbarHostState].
 */
@Composable
fun rememberShare(snackbarHostState: SnackbarHostState): (String) -> Unit {
    val sheet = rememberShareSheet()

    // ponytail: the deprecated ClipboardManager writes plain text on every platform; see AddByUrlDialog.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    return { text ->
        if (sheet != null) {
            sheet(text)
        } else {
            clipboard.setText(AnnotatedString(text))
            scope.launch { snackbarHostState.showSnackbar(getString(Res.string.link_copied)) }
        }
    }
}
