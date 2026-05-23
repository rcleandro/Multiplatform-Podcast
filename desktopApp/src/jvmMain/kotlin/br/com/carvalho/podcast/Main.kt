package br.com.carvalho.podcast

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.res.painterResource
import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.presentation.navigation.RootComponentImpl
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import androidx.compose.runtime.remember
import org.koin.mp.KoinPlatform.getKoin

fun main() {
    initKoin()
    val audioPlayer = getKoin().get<AudioPlayer>()

    application {
        val trayState = rememberTrayState()
        val playerState by audioPlayer.playerState.collectAsState()

        Tray(
            state = trayState,
            icon = painterResource("icon.png"),
            menu = {
                val isPlaying = playerState.isPlaying
                Item(
                    text = if (isPlaying) "Pause" else "Play",
                    onClick = {
                        if (isPlaying) audioPlayer.pause() else audioPlayer.resume()
                    }
                )
                Item(
                    text = "Next Track",
                    onClick = { audioPlayer.playNext() }
                )
                Item(
                    text = "Previous Track",
                    onClick = { audioPlayer.playPrevious() }
                )
                Separator()
                Item(
                    text = "Exit",
                    onClick = ::exitApplication
                )
            }
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "Podcast",
        ) {
            val lifecycle = remember { LifecycleRegistry() }
            val root = remember { RootComponentImpl(DefaultComponentContext(lifecycle = lifecycle)) }
            App(root)
        }
    }
}
