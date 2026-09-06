package app.plein.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.MonoMode
import app.plein.data.RingFolder
import app.plein.data.RingFolders
import app.plein.ui.icons.IconShape
import app.plein.ui.rememberHaptics
import app.plein.ui.theme.MonoFont

/**
 * Круговая папка на сетке.
 *
 * Диск тональной поверхности, по кольцу шесть значков, в середине — сколько
 * приложений осталось за ней. Значок в кольце запускается касанием, середина
 * раскрывает папку целиком: ради этого папка и круглая, шесть частых стоят
 * под пальцем и не требуют захода внутрь.
 *
 * Обводки у диска нет: глубину держит тон, как и везде в лаунчере.
 */
@Composable
fun RingFolderCell(
    folder: RingFolder,
    apps: Map<String, AppEntry>,
    repository: AppRepository,
    iconShape: IconShape,
    iconPack: String,
    monoMode: MonoMode,
    interactive: Boolean = true,
    onLaunch: (AppEntry) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val ring = folder.ring().mapNotNull { apps[it] }
    val rest = folder.appKeys.size - ring.size

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .testTag("ring:${folder.id}"),
        contentAlignment = Alignment.Center,
    ) {
        // Сторона папки — меньшая из двух: клетка 2×2 выше своей ширины на
        // место подписи значка, а кольцо обязано остаться кругом.
        val side = if (maxWidth < maxHeight) maxWidth else maxHeight
        val iconSize = side * RingFolders.ICON
        val coreSize = side * RingFolders.CORE

        Box(
            Modifier
                .size(side)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            ring.forEachIndexed { index, entry ->
                val (dx, dy) = RingFolders.offsetOf(index, ring.size)
                Box(
                    Modifier
                        .offset(x = side * dx, y = side * dy)
                        .size(iconSize)
                        .testTag("ring:${folder.id}:$index")
                        .clip(iconShape.shape())
                        .then(
                            if (interactive) {
                                Modifier.clickable {
                                    haptics.tick()
                                    onLaunch(entry)
                                }
                            } else {
                                Modifier
                            }
                        ),
                ) {
                    AppIcon(
                        entry = entry,
                        repository = repository,
                        size = iconSize,
                        iconShape = iconShape,
                        iconPack = iconPack,
                        monoMode = monoMode,
                    )
                }
            }

            Box(
                Modifier
                    .size(coreSize)
                    .clip(CircleShape)
                    .background(
                        if (rest > 0) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .then(
                        if (interactive) {
                            Modifier.clickable {
                                haptics.tick()
                                onOpen()
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (rest > 0) {
                    Text(
                        text = "+$rest",
                        fontFamily = MonoFont,
                        fontSize = (side.value * 0.075f).sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    // Пустая середина всё равно нажимается: точки говорят, что
                    // за ней что-то есть, даже когда остатка нет.
                    Dots(size = side.value * 0.026f)
                }
            }
        }
    }
}

@Composable
private fun Dots(size: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(size.dp * 0.9f)) {
        repeat(3) {
            Box(
                Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
