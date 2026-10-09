package br.com.carvalho.podcast.presentation.format

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.explicit
import org.jetbrains.compose.resources.stringResource

/** "Explicit" first in the line under a podcast or episode marked so (18.5); null otherwise. */
@Composable
fun explicitLabel(explicit: Boolean): String? = if (explicit) stringResource(Res.string.explicit) else null
