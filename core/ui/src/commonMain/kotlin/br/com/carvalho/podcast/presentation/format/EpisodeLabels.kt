package br.com.carvalho.podcast.presentation.format

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.episode_bonus
import br.com.carvalho.podcast.core.ui.generated.resources.episode_trailer
import br.com.carvalho.podcast.domain.model.EpisodeType
import org.jetbrains.compose.resources.stringResource

/** "Trailer" or "Bonus" first in the line under such an episode (18.10); null for a regular one. */
@Composable
fun episodeTypeLabel(type: EpisodeType): String? = when (type) {
    EpisodeType.TRAILER -> stringResource(Res.string.episode_trailer)
    EpisodeType.BONUS -> stringResource(Res.string.episode_bonus)
    EpisodeType.FULL -> null
}
