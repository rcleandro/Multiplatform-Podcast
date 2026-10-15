package br.com.carvalho.podcast.core.util

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformCapabilitiesTest {
    /** "Download only on Wi-Fi" sits among the download settings; it makes no sense where nothing downloads. */
    @Test
    fun wifiOnlyIsOfferedOnlyWhereThereAreDownloads() {
        assertTrue(!canLimitDownloadsToWifi || supportsDownloads)
    }
}
