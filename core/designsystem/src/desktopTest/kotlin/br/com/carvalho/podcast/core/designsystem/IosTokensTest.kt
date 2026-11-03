package br.com.carvalho.podcast.core.designsystem

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The iOS app's copy of this design system (ADR 0015, decision 4): the color sets of both themes, the spacing, sizes,
 * corner radii, opacities and type scale in `DesignTokens.swift`, and the Onest fonts. They come from the same Kotlin
 * values the Compose theme uses, so this test fails when the two drift apart. To write them again:
 * `./gradlew :core:designsystem:desktopTest --tests "*IosTokensTest" -Pios.tokens.write=true`.
 */
class IosTokensTest {
    private val ios = File(System.getProperty("user.dir")).resolve("../../iosApp/iosApp").normalize()
    private val write = System.getProperty("ios.tokens.write") == "true"

    @Test
    fun theIosAppHasTheSameColorsAsTheTheme() {
        val roles = colors(LightColorScheme).keys + colors(LightPodcastColors).keys
        val light = colors(LightColorScheme) + colors(LightPodcastColors)
        val dark = colors(DarkColorScheme) + colors(DarkPodcastColors)
        val theme = ios.resolve("Theme.xcassets/Theme")
        check(theme.resolve("Contents.json"), FOLDER)
        roles.forEach { role -> check(theme.resolve("$role.colorset/Contents.json"), colorSet(light.getValue(role), dark.getValue(role))) }
        if (!write) assertEquals(roles.map { "$it.colorset" }.toSet(), theme.list().orEmpty().filter { it.endsWith(".colorset") }.toSet())
    }

    @Test
    fun theIosAppHasTheSameSizesAndTypeAsTheTheme() = check(ios.resolve("DesignSystem/DesignTokens.swift"), swiftTokens())

    @Test
    fun theIosAppHasTheOnestFonts() {
        File("src/commonMain/composeResources/font").listFiles().orEmpty().sorted().forEach { font ->
            val copy = ios.resolve("Fonts/${font.name}")
            if (write) font.copyTo(copy, overwrite = true)
            assertEquals(font.readBytes().toList(), copy.readBytes().toList(), "$copy differs from $font")
        }
    }

    private fun check(file: File, expected: String) {
        if (write) {
            file.parentFile.mkdirs()
            file.writeText(expected)
        }
        assertEquals(expected, file.takeIf { it.exists() }?.readText(), "$file is out of date: see IosTokensTest")
    }

    // A color role's getter is mangled by the Color value class (`getPrimary-0d7_KjU`), and holds the color's bits.
    private fun colors(holder: Any): Map<String, Color> = holder.javaClass.methods
        .filter { it.name.startsWith("get") && "-" in it.name && it.parameterCount == 0 && it.returnType == Long::class.java }
        .associate { it.name.removePrefix("get").substringBefore("-").replaceFirstChar(Char::lowercase) to Color((it.invoke(holder) as Long).toULong()) }
        .toSortedMap()

    private fun colorSet(light: Color, dark: Color): String {
        fun hex(channel: Float) = "\"0x%02X\"".format(Locale.ROOT, (channel * CHANNEL).roundToInt())
        fun color(color: Color, pad: String) = listOf(
            "$pad\"color\" : {",
            "$pad  \"color-space\" : \"srgb\",",
            "$pad  \"components\" : {",
            "$pad    \"alpha\" : \"${"%.3f".format(Locale.ROOT, color.alpha)}\",",
            "$pad    \"blue\" : ${hex(color.blue)},",
            "$pad    \"green\" : ${hex(color.green)},",
            "$pad    \"red\" : ${hex(color.red)}",
            "$pad  }",
            "$pad},",
        )
        val lines = listOf("{", "  \"colors\" : [", "    {") + color(light, "      ") +
            listOf("      \"idiom\" : \"universal\"", "    },", "    {") +
            listOf(
                "      \"appearances\" : [",
                "        {",
                "          \"appearance\" : \"luminosity\",",
                "          \"value\" : \"dark\"",
                "        }",
                "      ],",
            ) + color(dark, "      ") +
            // The Apple Watch (ADR 0016) has no light and dark appearances and draws everything on black: the dark
            // colors, as the watch's own entry.
            listOf("      \"idiom\" : \"universal\"", "    },", "    {") + color(dark, "      ") +
            listOf("      \"idiom\" : \"watch\"", "    }", "  ],", "  \"info\" : {", "    \"author\" : \"xcode\",", "    \"version\" : 1", "  }", "}")
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun swiftTokens(): String {
        val typography = podcastTypography(FontFamily.Default)
        val extra = podcastExtraTypography(FontFamily.Default)
        val styles = listOf(
            "displayLarge" to typography.displayLarge, "displayMedium" to typography.displayMedium,
            "displaySmall" to typography.displaySmall, "headlineLarge" to typography.headlineLarge,
            "headlineMedium" to typography.headlineMedium, "headlineSmall" to typography.headlineSmall,
            "titleLarge" to typography.titleLarge, "titleMedium" to typography.titleMedium,
            "titleSmall" to typography.titleSmall, "bodyLarge" to typography.bodyLarge,
            "bodyMedium" to typography.bodyMedium, "bodySmall" to typography.bodySmall,
            "labelLarge" to typography.labelLarge, "labelMedium" to typography.labelMedium,
            "labelSmall" to typography.labelSmall, "timer" to extra.timer, "section" to extra.section,
        )
        val radii = listOf(
            "extraSmall" to Shapes.extraSmall, "small" to Shapes.small, "medium" to Shapes.medium,
            "large" to Shapes.large, "extraLarge" to Shapes.extraLarge,
        )
        return buildString {
            appendLine("// Generated by IosTokensTest from the Kotlin design system (ADR 0001, ADR 0015). Do not edit:")
            appendLine("// change the Kotlin values and write this file again (see IosTokensTest).")
            appendLine()
            appendLine("import CoreGraphics")
            appendLine()
            appendLine("enum Spacing {")
            dps(Spacing).forEach { (name, value) -> appendLine("    static let $name: CGFloat = ${number(value.value)}") }
            appendLine("}")
            appendLine()
            appendLine("enum Sizes {")
            dps(Sizes).forEach { (name, value) -> appendLine("    static let $name: CGFloat = ${number(value.value)}") }
            appendLine("}")
            appendLine()
            appendLine("enum Radius {")
            radii.forEach { (name, shape) -> appendLine("    static let $name: CGFloat = ${number(radius(shape))}") }
            appendLine("}")
            appendLine()
            appendLine("enum Opacity {")
            floats(Alpha).forEach { (name, value) -> appendLine("    static let $name: Double = ${number(value)}") }
            appendLine("}")
            appendLine()
            appendLine("enum Typography {")
            styles.forEach { (name, style) -> appendLine("    static let $name = ${textToken(name, style)}") }
            appendLine("}")
        }
    }

    private fun textToken(name: String, style: TextStyle): String {
        val size = style.fontSize.value
        val tracking = letterSpacing(style.letterSpacing, size)
        return "TextToken(font: \"${FONTS.getValue(style.fontWeight ?: FontWeight.Normal)}\", size: ${number(size)}, " +
            "lineHeight: ${number(style.lineHeight.value)}, tracking: ${number(tracking)}, " +
            "relativeTo: .${DYNAMIC_TYPE.getValue(name)}" + (if (style.fontFeatureSettings == "tnum") ", monospacedDigits: true" else "") + ")"
    }

    private fun letterSpacing(spacing: TextUnit, size: Float) = when {
        spacing == TextUnit.Unspecified -> 0f
        spacing.isSp -> spacing.value
        else -> spacing.value * size
    }

    private fun radius(shape: CornerBasedShape) = shape.topStart.toPx(Size(LARGE, LARGE), Density(1f))

    private fun dps(holder: Any) = holder.javaClass.methods
        .filter { it.name.startsWith("get") && "-" in it.name && it.parameterCount == 0 && it.returnType == Float::class.java }
        .map { it.name.removePrefix("get").substringBefore("-").replaceFirstChar(Char::lowercase) to Dp(it.invoke(holder) as Float) }
        .sortedBy { it.second.value }

    private fun floats(holder: Any) = holder.javaClass.declaredFields
        .filter { it.type == Float::class.java && java.lang.reflect.Modifier.isStatic(it.modifiers) }
        .map { it.name to it.getFloat(null) }
        .sortedBy { it.second }

    private fun number(value: Float): String {
        val rounded = (value * PRECISION).roundToInt() / PRECISION
        return if (rounded % 1 == 0f) rounded.toInt().toString() else rounded.toString()
    }

    private companion object {
        const val CHANNEL = 255
        const val LARGE = 1000f
        const val PRECISION = 1000f

        val FONTS = mapOf(
            FontWeight.Normal to "Onest-Regular",
            FontWeight.Medium to "Onest-Medium",
            FontWeight.SemiBold to "Onest-SemiBold",
            FontWeight.Bold to "Onest-Bold",
        )

        // The Dynamic Type style each one grows with, by its role.
        val DYNAMIC_TYPE = mapOf(
            "displayLarge" to "largeTitle", "displayMedium" to "largeTitle", "displaySmall" to "largeTitle",
            "headlineLarge" to "title", "headlineMedium" to "title", "headlineSmall" to "title2",
            "titleLarge" to "title2", "titleMedium" to "headline", "titleSmall" to "subheadline",
            "bodyLarge" to "body", "bodyMedium" to "callout", "bodySmall" to "footnote",
            "labelLarge" to "subheadline", "labelMedium" to "caption", "labelSmall" to "caption2",
            "timer" to "footnote", "section" to "caption2",
        )

        // A namespace, so `primary` and `secondary` do not clash with SwiftUI's own Color.primary and .secondary.
        val FOLDER = """
            {
              "info" : {
                "author" : "xcode",
                "version" : 1
              },
              "properties" : {
                "provides-namespace" : true
              }
            }

        """.trimIndent()
    }
}
