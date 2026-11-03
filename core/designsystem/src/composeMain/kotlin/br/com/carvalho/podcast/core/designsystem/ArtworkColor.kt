package br.com.carvalho.podcast.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlin.math.max
import kotlin.math.min

private const val SAMPLE_SIZE = 32
private const val MIN_SATURATION = 0.25f
private const val MIN_VALUE = 0.2f
private const val MIN_COLORFUL_SHARE = 0.05f
private const val TINT_STRONG = 0.35f
private const val TINT_SOFT = 0.2f
private const val TEXT_CONTRAST = 4.5f

/**
 * Average color of the pixels that carry color (not greys, whites or near black), or null when too few do,
 * for example a black-and-white cover. [pixels] are ARGB ints.
 */
fun dominantColor(pixels: IntArray): Color? {
    var r = 0f
    var g = 0f
    var b = 0f
    var count = 0
    for (argb in pixels) {
        val color = Color(argb)
        val high = max(color.red, max(color.green, color.blue))
        val low = min(color.red, min(color.green, color.blue))
        val saturation = if (high == 0f) 0f else (high - low) / high
        // White is already out through its zero saturation; only near-black needs the brightness floor.
        if (saturation >= MIN_SATURATION && high >= MIN_VALUE) {
            r += color.red
            g += color.green
            b += color.blue
            count++
        }
    }
    if (pixels.isEmpty() || count < pixels.size * MIN_COLORFUL_SHARE) return null
    return Color(red = r / count, green = g / count, blue = b / count)
}

/**
 * The cover color blended into [background], as strong as the [foreground] text allows while keeping
 * WCAG AA; null when even a soft tint would break the contrast, so the caller keeps the plain background.
 */
fun artworkTint(dominant: Color, background: Color, foreground: Color): Color? =
    listOf(TINT_STRONG, TINT_SOFT)
        .map { lerp(background, dominant, it) }
        .firstOrNull { contrastRatio(foreground, it) >= TEXT_CONTRAST }

internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + LUMINANCE_OFFSET) / (min(la, lb) + LUMINANCE_OFFSET)
}

private const val LUMINANCE_OFFSET = 0.05f

/** Android draws covers as hardware bitmaps, which cannot be read back; sampling needs a software one. */
internal expect fun ImageRequest.Builder.softwareBitmap(): ImageRequest.Builder

/** Loads [imageUrl] through Coil (sharing its cache) and returns its [dominantColor], or null. */
@Composable
fun rememberArtworkColor(imageUrl: String?): Color? {
    val context = LocalPlatformContext.current
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context).data(imageUrl).size(SAMPLE_SIZE).softwareBitmap().build()
    )
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val color by produceState<Color?>(initialValue = null, imageUrl) {
        if (imageUrl == null) return@produceState
        val success = painter.state.filterIsInstance<AsyncImagePainter.State.Success>().first()
        // Decoration only: if sampling fails on some platform, keep the plain background.
        value = runCatching { dominantColor(samplePixels(success.painter, density, direction)) }.getOrNull()
    }
    return color
}

private fun samplePixels(
    painter: androidx.compose.ui.graphics.painter.Painter,
    density: Density,
    direction: LayoutDirection,
): IntArray {
    val bitmap = ImageBitmap(SAMPLE_SIZE, SAMPLE_SIZE)
    val size = Size(SAMPLE_SIZE.toFloat(), SAMPLE_SIZE.toFloat())
    CanvasDrawScope().draw(density, direction, Canvas(bitmap), size) {
        with(painter) { draw(size) }
    }
    return IntArray(SAMPLE_SIZE * SAMPLE_SIZE).also { bitmap.readPixels(it) }
}
