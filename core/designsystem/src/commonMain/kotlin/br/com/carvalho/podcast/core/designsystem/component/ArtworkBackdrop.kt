package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import br.com.carvalho.podcast.core.designsystem.Motion
import br.com.carvalho.podcast.core.designsystem.artworkTint
import br.com.carvalho.podcast.core.designsystem.rememberArtworkColor

/**
 * Background tinted by the cover of [imageUrl]: a gradient from the cover color at the top to the plain
 * background. Falls back to the plain background when the cover has no clear color or the tint would make
 * the [MaterialTheme] text unreadable.
 */
@Composable
fun ArtworkBackdrop(imageUrl: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    val tint = rememberArtworkColor(imageUrl)?.let {
        artworkTint(it, background, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val top by animateColorAsState(tint ?: background, animationSpec = tween(Motion.MEDIUM, easing = Motion.Standard))
    // It paints the background, so it also sets the content color: the player is drawn outside any Scaffold.
    Box(modifier = modifier.background(Brush.verticalGradient(listOf(top, background)))) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground, content = content)
    }
}
