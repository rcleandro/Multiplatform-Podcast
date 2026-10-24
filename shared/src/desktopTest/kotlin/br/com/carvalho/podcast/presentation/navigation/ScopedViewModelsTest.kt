package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ScopedViewModelsTest {
    private val cleared = mutableListOf<String>()

    private inner class PageViewModel(val id: String) : ViewModel() {
        override fun onCleared() {
            cleared += id
        }
    }

    // Opening 30 podcasts one after the other kept 30 view models alive until the app closed (23.10).
    @Test
    fun onlyThePageOnScreenKeepsItsViewModel() = runComposeUiTest {
        var shown by mutableStateOf("p0")
        setContent {
            WithViewModels(rememberViewModelScope(shown)) {
                val owner = LocalViewModelStoreOwner.current!!
                val factory = viewModelFactory { initializer { PageViewModel(shown) } }
                ViewModelProvider.create(owner, factory)[PageViewModel::class]
            }
        }

        repeat(PAGES) { page ->
            shown = "p${page + 1}"
            waitForIdle()
        }

        assertEquals(List(PAGES) { "p$it" }, cleared)
    }

    private companion object {
        const val PAGES = 30
    }
}
