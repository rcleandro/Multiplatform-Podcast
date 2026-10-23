package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.generated.resources.Res
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_explicit
import org.jetbrains.compose.resources.stringResource

/** Finds the badge in tests, whatever the language. */
const val EXPLICIT_BADGE_TAG = "explicit_badge"

/** The feed marks this explicit (18.5): the "E" other podcast apps show, read aloud as the word. */
@Composable
fun ExplicitBadge(modifier: Modifier = Modifier) = Icon(
    imageVector = Icons.Rounded.Explicit,
    contentDescription = stringResource(Res.string.ds_explicit),
    tint = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier.size(Sizes.iconS).testTag(EXPLICIT_BADGE_TAG),
)
