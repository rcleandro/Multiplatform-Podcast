package br.com.carvalho.podcast.presentation.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.report_channels
import br.com.carvalho.podcast.core.ui.generated.resources.report_hide
import br.com.carvalho.podcast.core.ui.generated.resources.report_reason_violence
import br.com.carvalho.podcast.core.ui.generated.resources.report_send
import br.com.carvalho.podcast.domain.model.ReportReason
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ReportDialogTest {
    private fun text(res: StringResource) = runBlocking { getString(res) }

    @Test
    fun aReasonMustBePickedAndHidingCanBeLeftOut() = runComposeUiTest {
        var sent: Pair<ReportReason, Boolean>? = null
        setContent {
            PodcastTheme {
                ReportDialog(ReportTarget("Show", "https://feed"), onSend = { r, h -> sent = r to h }, onDismiss = {})
            }
        }

        onNodeWithText(text(Res.string.report_channels)).assertExists()
        onNodeWithText(text(Res.string.report_send)).assertIsNotEnabled()
        onNodeWithText(text(Res.string.report_reason_violence)).performClick()
        onNodeWithText(text(Res.string.report_hide)).performClick()
        onNodeWithText(text(Res.string.report_send)).assertIsEnabled().performClick()

        assertEquals(ReportReason.VIOLENCE to false, sent)
    }
}
