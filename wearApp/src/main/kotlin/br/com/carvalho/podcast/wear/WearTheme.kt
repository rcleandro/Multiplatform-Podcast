package br.com.carvalho.podcast.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import br.com.carvalho.podcast.core.designsystem.DarkColorScheme
import br.com.carvalho.podcast.core.designsystem.onestFontFamily

/**
 * The brand on the watch (19.9d): the phone's dark scheme (amber, sepia and slate on warm neutrals, ADR 0001) in
 * Wear's color roles, on the black a round OLED screen wants, and the Onest typeface in every style.
 */
@Composable
fun WearTheme(content: @Composable () -> Unit) {
    val family = onestFontFamily()
    val typography = remember(family) { brandTypography(family) }
    MaterialTheme(colorScheme = WearColorScheme, typography = typography, content = content)
}

/**
 * Wear's scale with the brand's typeface in every style: Wear names its own face (Roboto Flex) in each one, so a
 * default family alone would not reach them.
 */
private fun brandTypography(family: FontFamily): Typography = Typography().run {
    val arc = CurvedTextStyle(fontFamily = family)
    copy(
        arcLarge = arcLarge.merge(arc),
        arcMedium = arcMedium.merge(arc),
        arcSmall = arcSmall.merge(arc),
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        titleSmall = titleSmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        bodyExtraSmall = bodyExtraSmall.copy(fontFamily = family),
        numeralExtraLarge = numeralExtraLarge.copy(fontFamily = family),
        numeralLarge = numeralLarge.copy(fontFamily = family),
        numeralMedium = numeralMedium.copy(fontFamily = family),
        numeralSmall = numeralSmall.copy(fontFamily = family),
        numeralExtraSmall = numeralExtraSmall.copy(fontFamily = family),
    )
}

/** The phone's dark roles where Wear has the same ones; Wear's "dim" roles take the fixed-dim tones. */
internal val WearColorScheme = with(DarkColorScheme) {
    ColorScheme(
        primary = primary,
        primaryDim = primaryFixedDim,
        primaryContainer = primaryContainer,
        onPrimary = onPrimary,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryDim = secondaryFixedDim,
        secondaryContainer = secondaryContainer,
        onSecondary = onSecondary,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        tertiaryDim = tertiaryFixedDim,
        tertiaryContainer = tertiaryContainer,
        onTertiary = onTertiary,
        onTertiaryContainer = onTertiaryContainer,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        // Black (the dark scheme's scrim) lets an OLED screen turn its pixels off; Wear apps use it under everything.
        background = scrim,
        onBackground = onBackground,
        error = error,
        errorDim = error,
        errorContainer = errorContainer,
        onError = onError,
        onErrorContainer = onErrorContainer,
    )
}
