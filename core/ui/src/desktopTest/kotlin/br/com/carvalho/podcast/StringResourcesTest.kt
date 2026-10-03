package br.com.carvalho.podcast

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every text key must exist in the default (English), Portuguese and Spanish resources. */
class StringResourcesTest {

    private val root = File("src/commonMain/composeResources")
    private val keyPattern = Regex("""<(?:string|plurals) name="([^"]+)"""")

    private fun keys(locale: String): Set<String> =
        keyPattern.findAll(File(root, "$locale/strings.xml").readText()).map { it.groupValues[1] }.toSet()

    @Test
    fun everyLocaleHasTheSameKeys() {
        val default = keys("values")
        listOf("values-pt", "values-es").forEach { locale ->
            val other = keys(locale)
            assertEquals(emptySet(), default - other, "Missing in $locale")
            assertEquals(emptySet(), other - default, "Only in $locale")
        }
    }

    /** Compose resources only fill positional placeholders (%1$s); a bare %s would show up literally. */
    @Test
    fun placeholdersArePositional() {
        val bare = Regex("%[sdf]")
        listOf("values", "values-pt", "values-es").forEach { locale ->
            val offending = File(root, "$locale/strings.xml").readLines().filter { bare.containsMatchIn(it) }
            assertEquals(emptyList(), offending, "Non-positional placeholders in $locale")
        }
    }
}
