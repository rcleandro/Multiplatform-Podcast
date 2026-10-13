package br.com.carvalho.podcast.presentation.format

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.episode_bonus
import br.com.carvalho.podcast.core.ui.generated.resources.episode_trailer
import br.com.carvalho.podcast.core.ui.generated.resources.explicit
import br.com.carvalho.podcast.domain.model.EpisodeType
import org.jetbrains.compose.resources.stringResource

/** "Explicit" first in the line under a podcast or episode marked so (18.5); null otherwise. */
@Composable
fun explicitLabel(explicit: Boolean): String? = if (explicit) stringResource(Res.string.explicit) else null

/** "Trailer" or "Bonus" first in the line under such an episode (18.10); null for a regular one. */
@Composable
fun episodeTypeLabel(type: EpisodeType): String? = when (type) {
    EpisodeType.TRAILER -> stringResource(Res.string.episode_trailer)
    EpisodeType.BONUS -> stringResource(Res.string.episode_bonus)
    EpisodeType.FULL -> null
}
