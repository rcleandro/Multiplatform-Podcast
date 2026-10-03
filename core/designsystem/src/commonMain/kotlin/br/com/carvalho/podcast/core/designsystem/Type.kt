package br.com.carvalho.podcast.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import br.com.carvalho.podcast.core.designsystem.generated.resources.Res
import br.com.carvalho.podcast.core.designsystem.generated.resources.onest_bold
import br.com.carvalho.podcast.core.designsystem.generated.resources.onest_medium
import br.com.carvalho.podcast.core.designsystem.generated.resources.onest_regular
import br.com.carvalho.podcast.core.designsystem.generated.resources.onest_semibold
import org.jetbrains.compose.resources.Font

/** Text styles Material 3 has no slot for. Read them through [PodcastTheme.typography]. */
data class PodcastTypography(
    /** Playback times: tabular figures so "12:05" does not change width every second. */
    val timer: TextStyle,
    /** Section headers such as "Continuar ouvindo"; the caller upper-cases the text. */
    val section: TextStyle,
)

@Composable
internal fun onestFontFamily(): FontFamily = FontFamily(
    Font(Res.font.onest_regular, FontWeight.Normal),
    Font(Res.font.onest_medium, FontWeight.Medium),
    Font(Res.font.onest_semibold, FontWeight.SemiBold),
    Font(Res.font.onest_bold, FontWeight.Bold),
)

/**
 * The scale from ADR 0001. Styles the screens do not use keep the Material 3 sizes, but every style
 * gets the family, so components that pick them internally (snackbar, dialogs) do not fall back to the system font.
 */
internal fun podcastTypography(family: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.01).em,
        ),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}

internal fun podcastExtraTypography(family: FontFamily) = PodcastTypography(
    timer = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
    ),
    section = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.06.em,
    ),
)
