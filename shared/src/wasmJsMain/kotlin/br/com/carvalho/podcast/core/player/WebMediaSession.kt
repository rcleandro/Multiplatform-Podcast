package br.com.carvalho.podcast.core.player

@OptIn(ExperimentalWasmJsInterop::class)
private fun setTitle(value: JsString) {
    js("window.__mediaTitle = value")
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun setArtist(value: JsString) {
    js("window.__mediaArtist = value")
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun setArtworkUrl(value: JsString) {
    js("window.__mediaArtworkUrl = value")
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun applyMediaSession() {
    js("""
        if ('mediaSession' in navigator) {
            navigator.mediaSession.metadata = new MediaMetadata({
                title: window.__mediaTitle,
                artist: window.__mediaArtist,
                artwork: [
                    { src: window.__mediaArtworkUrl, sizes: '512x512', type: 'image/png' }
                ]
            });
        }
    """)
}
