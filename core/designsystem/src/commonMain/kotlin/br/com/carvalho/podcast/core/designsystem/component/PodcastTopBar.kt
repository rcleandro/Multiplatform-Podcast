package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Transparent top bar with an optional small title, used by the player and the detail screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastTopBar(
    navigationIcon: ImageVector,
    navigationContentDescription: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigate) { Icon(navigationIcon, contentDescription = navigationContentDescription) }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}
