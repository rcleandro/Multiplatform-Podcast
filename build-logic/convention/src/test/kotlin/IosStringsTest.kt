import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IosStringsTest {
    @Test
    fun composePlaceholdersAndEscapesBecomeApplesOwn() {
        assertEquals("%1\$@ of %2\$lld", IosStrings.apple("%1\$s of %2\$d"))
        assertEquals("Resume, %1\$lld%% played", IosStrings.apple("Resume, %1\$d%% played"))
        assertEquals("One\nTwo it's \"quoted\" é", IosStrings.apple("One\\nTwo it\\'s \\\"quoted\\\" \\u00e9"))
    }

    @Test
    fun eachFolderMapsToTheLanguageAppleKnows() {
        assertEquals("en", IosStrings.language("values"))
        assertEquals("pt", IosStrings.language("values-pt"))
        assertEquals("zh-Hans", IosStrings.language("values-zh"))
        assertNull(IosStrings.language("values-in"))
    }

    @Test
    fun theCatalogHoldsEveryLanguageAndThePlurals() {
        val catalog = IosStrings.catalog(
            mapOf(
                "en" to listOf(EN),
                "pt" to listOf("<resources><string name=\"library_title\">Biblioteca</string></resources>"),
            ),
        )

        assertTrue(catalog.startsWith("{\n  \"sourceLanguage\" : \"en\",\n  \"strings\" : {\n"))
        assertTrue(catalog.contains("\"pt\" : {\n          \"stringUnit\" : {\n            \"state\" : \"translated\",\n            \"value\" : \"Biblioteca\""))
        assertTrue(catalog.contains("\"one\" : {\n                \"stringUnit\" : {\n                  \"state\" : \"translated\",\n                  \"value\" : \"%1\$lld episode\""))
        assertTrue(catalog.contains("\"extractionState\" : \"manual\""))
        assertTrue(catalog.indexOf("\"library_title\"") < catalog.indexOf("\"podcast_episode_count\""))
    }

    private companion object {
        val EN = """
            <resources>
                <string name="library_title">Library</string>
                <plurals name="podcast_episode_count">
                    <item quantity="one">%1${'$'}d episode</item>
                    <item quantity="other">%1${'$'}d episodes</item>
                </plurals>
            </resources>
        """.trimIndent()
    }
}
