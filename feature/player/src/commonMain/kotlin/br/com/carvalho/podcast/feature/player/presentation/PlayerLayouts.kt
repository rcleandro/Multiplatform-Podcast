package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.TabletopFold
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.no_episode_selected
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The player of a foldable half open on a table (21.3): cover and title on the upright half, above [fold], and
 * progress and controls on the half lying down, where the fingers are.
 */
@Composable
internal fun TabletopPlayer(
    state: PlayerState,
    actions: PlayerActions,
    fold: TabletopFold,
    artworkModifier: Modifier,
) {
    // The fold comes in window pixels; where the player starts in the window says how much of it is above.
    var top by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val aboveFold = with(density) { (fold.top - top).coerceAtLeast(0).toDp() }
    val foldHeight = with(density) { (fold.bottom - fold.top).toDp() }
    Column(modifier = Modifier.fillMaxSize().onGloballyPositioned { top = it.positionInWindow().y.roundToInt() }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            // More room under the podcast's name, so it does not sit on the fold.
            modifier = Modifier
                .fillMaxWidth()
                .height(aboveFold)
                .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.s, bottom = Spacing.l),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                PlayerArtwork(state.currentEpisode, artworkModifier)
            }
            PlayerTitle(state.currentEpisode)
        }
        Spacer(modifier = Modifier.height(foldHeight))
        // Centered on the lower half while it fits, scrolling when it does not.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.l, Alignment.CenterVertically),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = Spacing.xl, vertical = Spacing.l),
            ) {
                PlayerInfoAndControls(state, actions, showTitle = false)
            }
        }
    }
}

/**
 * The player of a narrow and low window, like a flip phone's cover screen (21.5): no room for the cover beside the
 * controls, and the controls are what matter there.
 */
@Composable
internal fun CoverScreenPlayer(state: PlayerState, actions: PlayerActions) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        PlayerInfoAndControls(state, actions)
    }
}

/**
 * The cover on the left and the rest on the right, both centered. The controls always get the width they need, so
 * none is cut off; on a window too narrow for two equal halves the cover is the one that shrinks.
 */
@Composable
internal fun SideBySidePlayer(state: PlayerState, actions: PlayerActions, artworkModifier: Modifier, isShort: Boolean) {
    // Five controls with the large play button between them, plus some air.
    val controlsWidth = 320.dp
    val spacing = Spacing.xxl
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl, vertical = if (isShort) Spacing.s else Spacing.l),
    ) {
        val detailsWidth = maxOf((maxWidth - spacing) / 2, controlsWidth).coerceAtMost(maxWidth)
        val height = maxHeight
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxHeight()) {
                PlayerArtwork(state.currentEpisode, artworkModifier)
            }
            // Centered while it fits, scrolling when it does not.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Tighter on a short window, so speed, queue and timer fit under the controls too.
                verticalArrangement = Arrangement.spacedBy(
                    if (isShort) Spacing.m else Spacing.xl,
                    Alignment.CenterVertically,
                ),
                modifier = Modifier
                    .width(detailsWidth)
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = height),
            ) {
                PlayerInfoAndControls(state, actions)
            }
        }
    }
}

/** The episode's title over its podcast's. */
@Composable
internal fun PlayerTitle(episode: Episode?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = episode?.title ?: stringResource(Res.string.no_episode_selected),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        episode?.podcastTitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = PodcastTheme.colors.accentText,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
