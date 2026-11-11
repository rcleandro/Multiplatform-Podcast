package br.com.carvalho.podcast

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // The library keeps private feed URLs with their tokens: a cloud backup only when Google cannot read it (27.7).
    @Test
    fun theCloudBackupOnlyRunsEndToEndEncrypted() {
        val cloud = tags("dataExtractionRules").single { it.name == "cloud-backup" }
        assertEquals("true", cloud.attributes["disableIfNoEncryptionCapabilities"])

        val includes = tags("fullBackupContent").filter { it.name == "include" }
        assertEquals(setOf("file", "database", "sharedpref"), includes.map { it.attributes["domain"] }.toSet())
        assertTrue(includes.toString(), includes.all { it.attributes["requireFlags"] == "clientSideEncryption" })
    }

    private fun excludedPaths(attribute: String): List<String> =
        tags(attribute).filter { it.name == "exclude" }.map { "${it.attributes["domain"]}:${it.attributes["path"]}" }

    private data class Tag(val name: String, val attributes: Map<String, String>)

    private fun tags(attribute: String): List<Tag> {
        val name = Regex("""android:$attribute="@xml/(\w+)"""").find(manifest)?.groupValues?.get(1) ?: return emptyList()
        val parser = context.resources.getXml(context.resources.getIdentifier(name, "xml", context.packageName))
        val tags = mutableListOf<Tag>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                val attributes = (0 until parser.attributeCount).associate {
                    parser.getAttributeName(it).substringAfter(':') to parser.getAttributeValue(it)
                }
                tags += Tag(parser.name, attributes)
            }
        }
        return tags
    }

    private companion object {
        // AppDirectories.downloadsDir, under filesDir.
        const val DOWNLOADS = "file:downloads/"
    }
}
