package br.com.carvalho.podcast.wear

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import coil3.compose.AsyncImage

/** A cover in a list's icon slot (19.9g), as the watch's media apps show them. */
@Composable
fun Thumbnail(imageUrl: String?) {
    val size = 32.dp
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).clip(MaterialTheme.shapes.small),
    )
}
