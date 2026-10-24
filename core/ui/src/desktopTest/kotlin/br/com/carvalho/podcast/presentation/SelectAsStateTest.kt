package br.com.carvalho.podcast.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.domain.model.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SelectAsStateTest {
    private data class Playing(val episode: String, val position: Long)

    @Test
    fun aScreenThatShowsTheEpisodeIgnoresThePositionMoving() = runComposeUiTest {
        val state = MutableStateFlow(Playing("e1", 0))
        var compositions = 0
        setContent {
            val episode by state.selectAsState { it.episode }
            compositions++
            Text(episode)
        }

        repeat(TICKS) {
            state.value = state.value.copy(position = state.value.position + TICK_MS)
            waitForIdle()
        }
        assertEquals(1, compositions)

        state.value = state.value.copy(episode = "e2")
        waitForIdle()
        assertEquals(2, compositions)
    }

    @Test
    fun anEpisodeListSeesTheSamePlayerWhateverThePosition() {
        val playing = PlayerState(isPlaying = true, position = 1_000, duration = 60_000, speed = 1.5f)

        assertEquals(playing.forRows(), playing.copy(position = 31_000).forRows())
        assertEquals(true, playing.forRows().isPlaying)
    }

    private companion object {
        const val TICKS = 10
        const val TICK_MS = 500L
    }
}
