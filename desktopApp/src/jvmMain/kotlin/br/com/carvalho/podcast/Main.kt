package br.com.carvalho.podcast

import org.jetbrains.compose.resources.painterResource
import br.com.carvalho.podcast.core.ui.generated.resources.app_icon
import org.jetbrains.compose.resources.stringResource
import br.com.carvalho.podcast.core.ui.generated.resources.tray_quit
import br.com.carvalho.podcast.core.ui.generated.resources.tray_previous
import br.com.carvalho.podcast.core.ui.generated.resources.tray_next
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.app_name
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
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
            icon = painterResource(Res.drawable.app_icon),
            menu = {
                val isPlaying = playerState.isPlaying
                Item(
                    text = stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                    onClick = {
                        if (isPlaying) audioPlayer.pause() else audioPlayer.resume()
                    }
                )
                Item(
                    text = stringResource(Res.string.tray_next),
                    onClick = { audioPlayer.playNext() }
                )
                Item(
                    text = stringResource(Res.string.tray_previous),
                    onClick = { audioPlayer.playPrevious() }
                )
                Separator()
                Item(
                    text = stringResource(Res.string.tray_quit),
                    onClick = ::exitApplication
                )
            }
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.app_icon),
        ) {
            val lifecycle = remember { LifecycleRegistry() }
            val root = remember { RootComponentImpl(DefaultComponentContext(lifecycle = lifecycle)) }
            App(root)
        }
    }
}
