package br.com.carvalho.podcast.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    podcastDefaults(platform = "Desktop")
}
