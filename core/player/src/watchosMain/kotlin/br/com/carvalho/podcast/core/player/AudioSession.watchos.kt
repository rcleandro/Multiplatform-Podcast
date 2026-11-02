package br.com.carvalho.podcast.core.player

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeSpokenAudio
import platform.AVFAudio.AVAudioSessionRouteSharingPolicyLongFormAudio
import platform.AVFAudio.activateWithOptions
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Long-form audio (ADR 0016, decision 5): the watch plays it on Bluetooth headphones or speakers, never on its own
 * speaker, the Wear OS app's "headphones only" (19.9f).
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun setUpAudioSession() {
    AVAudioSession.sharedInstance().setCategory(
        AVAudioSessionCategoryPlayback,
        mode = AVAudioSessionModeSpokenAudio,
        routeSharingPolicy = AVAudioSessionRouteSharingPolicyLongFormAudio,
        options = 0u,
        error = null,
    )
}

/** Asks the system for an output; nothing plays if the person closes the picker without choosing one. */
internal actual fun whenAudioCanPlay(start: () -> Unit) {
    AVAudioSession.sharedInstance().activateWithOptions(0u) { activated: Boolean, _ ->
        if (activated) dispatch_async(dispatch_get_main_queue()) { start() }
    }
}
