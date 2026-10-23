package br.com.carvalho.podcast.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BrowserFetchFailureTest {
    // The data layer recognises a feed the browser refused (CORS, 20.4) by this exact Error; a Ktor update that
    // changes it would turn the explanation back into a generic failure.
    @Test
    fun aFetchTheBrowserRefusesIsAnErrorWithKtorsMessage() = runTest {
        val error = assertFailsWith<Error> { HttpClient(Js).get("http://127.0.0.1:9/feed.xml") }

        assertEquals("Fail to fetch", error.message)
    }
}
