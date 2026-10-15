package br.com.carvalho.podcast

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every text key must exist in the default (English) resources and in every language the app speaks (18.20). */
class StringResourcesTest {

    /** This module's texts and the design system's. */
    private val roots = listOf(
        File("src/commonMain/composeResources"),
        File("../designsystem/src/commonMain/composeResources"),
    )
    private val keyPattern = Regex("""<(?:string|plurals) name="([^"]+)"""")

    private fun keys(file: File): Set<String> = keyPattern.findAll(file.readText()).map { it.groupValues[1] }.toSet()

    /** Each module's strings.xml per folder ("values", "values-pt"…). */
    private val files: List<Pair<File, File>> = roots.flatMap { root ->
        root.listFiles { file -> file.name.startsWith("values") }.orEmpty()
            .map { root to File(it, "strings.xml") }
            .filter { it.second.exists() }
    }

    @Test
    fun everyLocaleHasTheSameKeys() {
        roots.forEach { root ->
            val default = keys(File(root, "values/strings.xml"))
            files.filter { it.first == root }.forEach { (_, file) ->
                val other = keys(file)
                assertEquals(emptySet(), default - other, "Missing in $file")
                assertEquals(emptySet(), other - default, "Only in $file")
            }
        }
    }

    /** The languages Settings offer have their folder; Indonesian has Android's old code too. */
    @Test
    fun everyLanguageOfferedHasItsTexts() {
        val folders = files.map { it.second.parentFile.name }.toSet()
        val expected = listOf("pt", "es", "fr", "de", "it", "ja", "ko", "zh", "hi", "ar", "id", "in", "ru", "tr")
        assertEquals(emptyList(), expected.filterNot { "values-$it" in folders })
    }

    /** Compose resources only fill positional placeholders (%1$s); a bare %s would show up literally. */
    @Test
    fun placeholdersArePositional() {
        val bare = Regex("%[sdf]")
        files.forEach { (_, file) ->
            // "%%" is a literal percent sign, not a placeholder.
            val offending = file.readLines().filter { bare.containsMatchIn(it.replace("%%", "")) }
            assertEquals(emptyList(), offending, "Non-positional placeholders in $file")
        }
    }

    /** Compose resources keep the backslash of \" (Android resources drop it), so the user saw it on screen. */
    @Test
    fun quotesAreNotEscaped() {
        files.forEach { (_, file) ->
            val offending = file.readLines().filter { "\\\"" in it || "\\'" in it }
            assertEquals(emptyList(), offending, "Escaped quotes in $file")
        }
    }
}
