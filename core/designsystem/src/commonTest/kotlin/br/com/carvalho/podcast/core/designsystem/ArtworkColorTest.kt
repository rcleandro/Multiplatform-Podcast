package br.com.carvalho.podcast.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArtworkColorTest {

    private fun pixels(vararg colors: Pair<Color, Int>) =
        colors.flatMap { (color, n) -> List(n) { color.toArgb() } }.toIntArray()

    @Test
    fun ignoresGreysAndExtremesWhenAveraging() {
        val teal = Color(red = 0.1f, green = 0.6f, blue = 0.6f)
        val result = dominantColor(pixels(teal to 30, Color.White to 40, Color.Black to 20, Color.Gray to 10))

        assertNotNull(result)
        assertEquals(teal.toArgb(), result.toArgb())
    }

    @Test
    fun blackAndWhiteCoverHasNoDominantColor() {
        assertNull(dominantColor(pixels(Color.White to 50, Color.Black to 50, Color.Gray to 50)))
    }

    @Test
    fun tintKeepsTextReadableOrGivesUp() {
        val background = Color(0xFF161513)
        val text = Color(0xFFF1EFEA)
        val tint = artworkTint(Color(red = 0.9f, green = 0.3f, blue = 0.1f), background, text)

        assertNotNull(tint)
        assertTrue(contrastRatio(text, tint) >= 4.5f)
        // A light text on a light, saturated cover cannot reach AA even with the soft tint.
        assertNull(artworkTint(Color.Yellow, Color.White, Color(0xFFF1EFEA)))
    }

    @Test
    fun brightSaturatedColorsCount() {
        val cyan = Color(red = 0.1f, green = 0.75f, blue = 0.95f)
        val result = dominantColor(pixels(cyan to 40, Color(0xFF333333) to 60))

        assertNotNull(result)
        assertEquals(cyan.toArgb(), result.toArgb())
    }
}
