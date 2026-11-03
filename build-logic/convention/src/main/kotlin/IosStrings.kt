import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Writes the iOS app's `Localizable.xcstrings` from the `composeResources` strings (ADR 0015, decision 3), which stay
 * the only place texts are written. With [checkOnly], fails instead when the file in the repository is out of date.
 */
abstract class IosStringsTask : DefaultTask() {
    /** The `composeResources` folders, each with its `values*` folders. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resources: ConfigurableFileCollection

    // An output of the generator and an input of the check, so it is declared where each one is registered.
    @get:Internal
    abstract val catalog: RegularFileProperty

    @get:Input
    abstract val checkOnly: Property<Boolean>

    @TaskAction
    fun run() {
        val sources = resources.files
            .flatMap { dir -> dir.listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("values") } }
            .flatMap { values ->
                val language = IosStrings.language(values.name) ?: return@flatMap emptyList()
                values.listFiles { file -> file.extension == "xml" }.orEmpty().sorted().map { language to it.readText() }
            }
            .groupBy({ it.first }, { it.second })
        val text = IosStrings.catalog(sources)
        val target = catalog.get().asFile
        if (!checkOnly.get()) {
            target.writeText(text)
        } else if (!target.exists() || target.readText() != text) {
            throw GradleException("$target is out of date with the composeResources: run ./gradlew generateIosStrings")
        }
    }
}

/** Registers `generateIosStrings` and `checkIosStrings` over [resources]; `check` runs the second. */
fun Project.iosStrings(resources: List<File>, catalog: File) {
    tasks.register("generateIosStrings", IosStringsTask::class.java) {
        this.resources.from(resources)
        this.catalog.set(catalog)
        checkOnly.set(false)
        outputs.file(catalog)
    }
    val check = tasks.register("checkIosStrings", IosStringsTask::class.java) {
        this.resources.from(resources)
        this.catalog.set(catalog)
        checkOnly.set(true)
        inputs.file(catalog)
    }
    tasks.named("check") { dependsOn(check) }
}

object IosStrings {
    const val SOURCE = "en"

    // values-in is Android's old code for Indonesian, kept next to values-id; Apple only knows id.
    private val SKIPPED = setOf("in")
    private val APPLE_NAMES = mapOf("zh" to "zh-Hans")
    private val PLACEHOLDER = Regex("""%(\d+\$)?([sd])""")
    private val ESCAPE = Regex("""\\(u[0-9a-fA-F]{4}|.)""")
    private const val HEX = 16

    /** The Apple language of a `values-xx` folder, or null for one the iOS app leaves out. */
    fun language(folder: String): String? {
        val qualifier = folder.removePrefix("values").removePrefix("-")
        return when {
            qualifier.isEmpty() -> SOURCE
            qualifier in SKIPPED -> null
            else -> APPLE_NAMES[qualifier] ?: qualifier
        }
    }

    /** The catalog of [sources] (each language's `strings.xml` files), laid out as Xcode writes it. */
    fun catalog(sources: Map<String, List<String>>): String {
        val texts = sortedMapOf<String, MutableMap<String, Json>>()
        sources.forEach { (language, xmls) ->
            xmls.forEach { xml -> read(xml).forEach { (key, text) -> texts.getOrPut(key) { sortedMapOf() }[language] = text } }
        }
        val strings = texts.map { (key, languages) ->
            key to Obj("extractionState" to Str("manual"), "localizations" to Obj(languages.toList()))
        }
        return Obj("sourceLanguage" to Str(SOURCE), "strings" to Obj(strings), "version" to Str("1.0")).write(0) + "\n"
    }

    /** A Compose text as Apple writes it: `%1$s` is `%1$@`, `%1$d` is `%1$lld`, and the escapes are undone. */
    fun apple(text: String): String {
        val unescaped = ESCAPE.replace(text) { match ->
            val code = match.groupValues[1]
            when {
                code == "n" -> "\n"
                code == "t" -> "\t"
                code.length > 1 -> code.drop(1).toInt(HEX).toChar().toString()
                else -> code
            }
        }
        return PLACEHOLDER.replace(unescaped) { "%" + it.groupValues[1] + if (it.groupValues[2] == "s") "@" else "lld" }
    }

    private fun read(xml: String): List<Pair<String, Json>> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
            .documentElement.childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }.mapNotNull { element ->
            val key = element.getAttribute("name")
            when (element.tagName) {
                "string" -> key to unit(apple(element.textContent))
                "plurals" -> {
                    val items = element.getElementsByTagName("item")
                    val forms = (0 until items.length).map { items.item(it) as Element }
                        .associate { it.getAttribute("quantity") to unit(apple(it.textContent)) }.toSortedMap()
                    key to Obj("variations" to Obj("plural" to Obj(forms.toList())))
                }
                else -> null
            }
        }
    }

    private fun unit(value: String) = Obj("stringUnit" to Obj("state" to Str("translated"), "value" to Str(value)))

    // A small JSON writer, so the layout is Xcode's own (`"key" : value`, two spaces) and opening the catalog in Xcode
    // leaves it as it is.
    private sealed interface Json {
        fun write(depth: Int): String
    }

    private class Str(val value: String) : Json {
        override fun write(depth: Int) = quote(value)
    }

    private class Obj(val members: List<Pair<String, Json>>) : Json {
        constructor(vararg members: Pair<String, Json>) : this(members.toList())

        override fun write(depth: Int): String {
            val pad = "  ".repeat(depth + 1)
            val body = members.joinToString(",\n") { (key, value) -> "$pad${quote(key)} : ${value.write(depth + 1)}" }
            return "{\n$body\n${"  ".repeat(depth)}}"
        }
    }

    private fun quote(text: String) = buildString {
        append('"')
        text.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }
}
