package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import br.com.carvalho.podcast.core.designsystem.component.parseHtml
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.headphones_only
import br.com.carvalho.podcast.core.ui.generated.resources.playback_speed
import br.com.carvalho.podcast.core.ui.generated.resources.settings_boost_voice
import br.com.carvalho.podcast.core.ui.generated.resources.settings_skip_silence
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.presentation.format.formatSpeed
import br.com.carvalho.podcast.presentation.format.relativeTime
import br.com.carvalho.podcast.presentation.format.text
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource

/**
 * The player's second page (19.9h), a swipe up from the controls: the episode's cover, title, podcast, when it came
 * out and how long it is, the speed and the sleep timer as on the phone's player, the sound's switches (19.9f) and
 * the episode's description.
 */
@Composable
fun EpisodeInfoPage(
    state: PlayerState,
    timerLabel: String?,
    onSpeed: () -> Unit,
    onTimer: () -> Unit,
    effects: WatchEffects = WatchEffects(),
    onEffects: (WatchEffects) -> Unit = {},
) {
    val coverSize = 96.dp
    val episode = state.currentEpisode
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                AsyncImage(
                    model = episode?.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(coverSize).clip(MaterialTheme.shapes.medium),
                )
            }
            item {
                Text(
                    episode?.title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                val published = episode?.let { relativeTime(it.publishDate, getCurrentTimestamp()) }?.text()
                val length = episode?.duration?.takeIf { it > 0 }?.toDuration()
                Text(
                    listOfNotNull(episode?.podcastTitle, published, length).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                val speed = stringResource(Res.string.playback_speed)
                OptionButton(Icons.Rounded.Speed, speed, formatSpeed(state.speed), onSpeed)
            }
            item { OptionButton(Icons.Rounded.Bedtime, stringResource(Res.string.sleep_timer), timerLabel, onTimer) }
            soundSwitches(effects, onEffects)
            episode?.description?.let { description ->
                item {
                    val text = remember(description) { parseHtml(description) }
                    Text(text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** One of the player's options, with what it is set to. */
@Composable
private fun OptionButton(icon: ImageVector, label: String, value: String?, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        secondaryLabel = value?.let { { Text(it) } },
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The sound's switches (19.9f): the phone's speech effects, and headphones only. */
private fun ScalingLazyListScope.soundSwitches(effects: WatchEffects, onEffects: (WatchEffects) -> Unit) {
    item {
        Toggle(stringResource(Res.string.settings_skip_silence), effects.skipSilence) {
            onEffects(effects.copy(skipSilence = it))
        }
    }
    item {
        Toggle(stringResource(Res.string.settings_boost_voice), effects.boostVoice) {
            onEffects(effects.copy(boostVoice = it))
        }
    }
    item {
        Toggle(stringResource(Res.string.headphones_only), effects.headphonesOnly) {
            onEffects(effects.copy(headphonesOnly = it))
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}
