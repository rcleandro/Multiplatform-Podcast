package br.com.carvalho.podcast.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

// OkHttp, not the Android engine: HttpURLConnection is not thread safe, and cancelling a download closed its stream
// while another thread was reading it ("Unbalanced enter/exit"), which crashed the app.
actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    podcastDefaults(platform = "Android")
}
