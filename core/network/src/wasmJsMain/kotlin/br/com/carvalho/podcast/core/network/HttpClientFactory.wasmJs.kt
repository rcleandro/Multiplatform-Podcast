package br.com.carvalho.podcast.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun createHttpClient(): HttpClient = HttpClient(Js) {
    podcastDefaults(platform = null)
}
