package br.com.carvalho.podcast.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ReadableWidthTest {

    @Test
    fun onAWideWindowTheContentStopsAtTheReadableWidthCentered() = runComposeUiTest {
        val window = 1000.dp
        setContent {
            Box(modifier = Modifier.requiredWidth(window)) {
                Box(modifier = Modifier.readableWidth().testTag(CONTENT))
            }
        }

        onNodeWithTag(CONTENT).assertWidthIsEqualTo(Sizes.readableWidth)
        onNodeWithTag(CONTENT).assertLeftPositionInRootIsEqualTo((window - Sizes.readableWidth) / 2)
    }

    @Test
    fun onAPhoneTheContentTakesTheWholeWidth() = runComposeUiTest {
        val phone = 400.dp
        setContent {
            Box(modifier = Modifier.requiredWidth(phone)) {
                Box(modifier = Modifier.readableWidth().testTag(CONTENT))
            }
        }

        onNodeWithTag(CONTENT).assertWidthIsEqualTo(phone)
    }

    private companion object {
        const val CONTENT = "content"
    }
}
