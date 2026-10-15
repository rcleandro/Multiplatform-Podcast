package br.com.carvalho.podcast

import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.link_copied
import br.com.carvalho.podcast.presentation.share.rememberShare
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ShareTest {
    /** A computer has no share sheet: the text is copied and the screen says so (18.4). */
    @Test
    fun withoutAShareSheetTheTextIsCopied() = runComposeUiTest {
        var copied: String? = null
        @Suppress("DEPRECATION")
        val clipboard = object : ClipboardManager {
            override fun getText() = copied?.let(::AnnotatedString)
            override fun setText(annotatedString: AnnotatedString) {
                copied = annotatedString.text
            }
        }
        setContent {
            @Suppress("DEPRECATION")
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                val snackbar = remember { SnackbarHostState() }
                val share = rememberShare(snackbar)
                Column {
                    Button(onClick = { share("Hipsters\nhttps://hipsters.tech") }) { Text("share") }
                    SnackbarHost(snackbar)
                }
            }
        }

        onNodeWithText("share").performClick()

        assertEquals("Hipsters\nhttps://hipsters.tech", copied)
        onNodeWithText(runBlocking { getString(Res.string.link_copied) }).assertExists()
    }
}
