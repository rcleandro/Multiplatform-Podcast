package br.com.carvalho.podcast.core.designsystem

import androidx.compose.ui.unit.dp

/** The one spacing scale (ADR 0001) for margins, paddings and gaps. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/**
 * Sizes that several components share. A size that belongs to one composable stays a named `val` inside it.
 */
object Sizes {
    val touchTarget = 48.dp
    val artworkS = 56.dp
    val artworkM = 140.dp
    val artworkL = 280.dp
    val miniPlayerHeight = 64.dp
    val playButtonLarge = 72.dp

    /** Bottom padding of scrolling lists so the last item is not hidden behind the mini player. */
    val listBottomInset = miniPlayerHeight + Spacing.l

    /** Extra bottom padding of lists under a floating action button: its height (56) and its margin. */
    val fabClearance = 56.dp + Spacing.l

    val iconS = 18.dp
    val iconM = 24.dp
    val iconL = 32.dp
    val iconXl = 40.dp

    /** Stroke of the small circular progress indicators inside buttons. */
    val progressStroke = 2.dp
}

object Alpha {
    const val disabled = 0.38f
    const val muted = 0.6f
    const val scrim = 0.32f

    /** Tracks and tints drawn over a colored container. */
    const val faint = 0.12f
}
