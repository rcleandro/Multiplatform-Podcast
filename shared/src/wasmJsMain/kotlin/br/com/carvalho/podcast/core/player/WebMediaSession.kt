package br.com.carvalho.podcast.core.player

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(title, artist, artworkUrl) => {
    if ('mediaSession' in navigator) {
        navigator.mediaSession.metadata = new MediaMetadata({
            title: title,
            artist: artist,
            artwork: [
                { src: artworkUrl, sizes: '512x512', type: 'image/png' }
            ]
        });
    }
}""")
external fun updateMediaSessionMetadata(title: String, artist: String, artworkUrl: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(onPlay, onPause, onSeekBackward, onSeekForward, onPreviousTrack, onNextTrack) => {
    if ('mediaSession' in navigator) {
        navigator.mediaSession.setActionHandler('play', () => onPlay());
        navigator.mediaSession.setActionHandler('pause', () => onPause());
        navigator.mediaSession.setActionHandler('seekbackward', () => onSeekBackward());
        navigator.mediaSession.setActionHandler('seekforward', () => onSeekForward());
        navigator.mediaSession.setActionHandler('previoustrack', () => onPreviousTrack());
        navigator.mediaSession.setActionHandler('nexttrack', () => onNextTrack());
    }
}""")
external fun setupMediaSessionActions(
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onPreviousTrack: () -> Unit,
    onNextTrack: () -> Unit
)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(state) => {
    if ('mediaSession' in navigator) {
        navigator.mediaSession.playbackState = state;
    }
}""")
external fun updateMediaSessionPlaybackState(state: String)
