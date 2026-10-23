package br.com.carvalho.podcast

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import java.io.File

@RunWith(RobolectricTestRunner::class)
// A bare application: the real one starts the player and the sync.
@Config(sdk = [35], application = Application::class)
class BackupRulesTest {
    private val context = RuntimeEnvironment.getApplication()

    // Robolectric does not fill the backup fields of ApplicationInfo, so the test reads the manifest itself.
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun downloadedEpisodesStayOutOfEveryBackup() {
        val downloads = listOf(DOWNLOADS)

        assertEquals(downloads + downloads, excludedPaths("dataExtractionRules"))
        assertEquals(downloads, excludedPaths("fullBackupContent"))
    }

    private fun excludedPaths(attribute: String): List<String> {
        val name = Regex("""android:$attribute="@xml/(\w+)"""").find(manifest)?.groupValues?.get(1) ?: return emptyList()
        val parser = context.resources.getXml(context.resources.getIdentifier(name, "xml", context.packageName))
        val paths = mutableListOf<String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "exclude") {
                paths += parser.getAttributeValue(null, "domain") + ":" + parser.getAttributeValue(null, "path")
            }
        }
        return paths
    }

    private companion object {
        // AppDirectories.downloadsDir, under filesDir.
        const val DOWNLOADS = "file:downloads/"
    }
}
