package app.plein.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.plein.data.AppEntry
import app.plein.data.AppRepository

/**
 * Значок приложения, обрезанный выбранной формой.
 * Битмап тянется из кэша репозитория, поэтому прокрутка не грузит диск.
 */
@Composable
fun AppIcon(
    entry: AppEntry,
    repository: AppRepository,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    var bitmap by remember(entry.key, px) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(entry.key, px) {
        bitmap = repository.icon(entry, px)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
