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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.MonoMode
import app.plein.data.MonoStyle
import app.plein.ui.icons.IconShape

/**
 * Значок приложения, обрезанный выбранной формой.
 * Битмап тянется из кэша репозитория, поэтому прокрутка не грузит диск.
 */
@Composable
fun AppIcon(
    entry: AppEntry,
    repository: AppRepository,
    size: Dp,
    iconShape: IconShape,
    iconPack: String = "",
    monoMode: MonoMode = MonoMode.Off,
    modifier: Modifier = Modifier,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val shapeKey = iconShape.name
    // Цвета монохрома берутся из темы прямо здесь: они входят в ключ кэша,
    // поэтому смена палитры сама тянет за собой перерисовку значков.
    val scheme = MaterialTheme.colorScheme
    // Цвета отдаём всегда: даже при выключенном монохроме линейный пак нужно
    // красить под тему, иначе он остаётся белым на любой палитре.
    val mono = MonoStyle(
        mode = monoMode,
        tint = scheme.onSurface.toArgb(),
        background = scheme.surfaceContainerHighest.toArgb(),
    )

    // Готовый значок берём из памяти сразу: корутина на каждую ячейку роняла
    // быструю прокрутку. Форма уже вжжена в битмап, клипа на экране нет.
    val cached = repository.cachedIcon(entry, px, shapeKey, iconPack, mono)
    var bitmap by remember(entry.key, px, shapeKey, iconPack, mono) { mutableStateOf(cached) }

    LaunchedEffect(entry.key, px, shapeKey, iconPack, mono) {
        if (bitmap == null) {
            bitmap = repository.icon(entry, px, shapeKey, iconShape.path(px), iconPack, mono)
        }
    }

    Box(modifier = modifier.size(size)) {
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
