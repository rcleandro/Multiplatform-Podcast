package br.com.carvalho.podcast.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.settings
import br.com.carvalho.podcast.domain.model.AppLanguage
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppLanguagesTest {
    private val systemLocale = Locale.getDefault()

    @AfterTest
    fun tearDown() = Locale.setDefault(systemLocale)

    @Test
    fun theChosenLanguageAnswersAtOnceAndTheSystemsComesBack() = runComposeUiTest {
        var language by mutableStateOf(AppLanguage.SPANISH)
        setContent { ProvideAppLanguage(language) { Text(stringResource(Res.string.settings)) } }

        onNodeWithText("Ajustes").assertExists()

        language = AppLanguage.PORTUGUESE
        onNodeWithText("Configurações").assertExists()

        language = AppLanguage.SYSTEM
        waitForIdle()
        assertEquals(systemLocale, Locale.getDefault())
    }

    @Test
    fun arabicTurnsTheLayoutRightToLeftAndTheOthersLeftToRight() = runComposeUiTest {
        var language by mutableStateOf(AppLanguage.ARABIC)
        var direction: LayoutDirection? = null
        setContent {
            ProvideAppLanguage(language) {
                ProvideLayoutDirection {
                    direction = LocalLayoutDirection.current
                    Text(stringResource(Res.string.settings))
                }
            }
        }

        onNodeWithText("الإعدادات").assertExists()
        assertEquals(LayoutDirection.Rtl, direction)

        language = AppLanguage.JAPANESE
        onNodeWithText("設定").assertExists()
        assertEquals(LayoutDirection.Ltr, direction)
    }
}
