package app.plein.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.plein.data.AppEntry
import app.plein.data.AppRepository

/**
 * Сетка приложений.
 *
 * В режиме правки значок берётся долгим нажатием и переносится: порядок это
 * то, как человек разложил, а не как отсортировала система.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsGrid(
    apps: List<AppEntry>,
    repository: AppRepository,
    columns: Int,
    iconSize: Dp,
    iconShape: app.plein.ui.icons.IconShape,
    showLabels: Boolean,
    editing: Boolean,
    onReorder: (List<AppEntry>) -> Unit,
    onClick: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(apps) { apps.toMutableStateList() }
    val gridState = rememberLazyGridState()

    // Ключ и точки касания вместо накопленного сдвига: значок обязан оставаться
    // под пальцем, в том числе после перестановки соседей.
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var pointerStart by remember { mutableStateOf(Offset.Zero) }
    var pointerNow by remember { mutableStateOf(Offset.Zero) }
    var anchorOffset by remember { mutableStateOf(IntOffset.Zero) }

    val haptics = LocalHapticFeedback.current

    LaunchedEffect(editing) {
        if (!editing) draggedKey = null
    }

    fun itemAt(position: Offset): LazyGridItemInfo? =
        gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
            position.x >= info.offset.x && position.x <= info.offset.x + info.size.width &&
                position.y >= info.offset.y && position.y <= info.offset.y + info.size.height
        }

    fun offsetOf(key: String): IntOffset? =
        gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
            ?.let { IntOffset(it.offset.x, it.offset.y) }

    val dragModifier = if (!editing) Modifier else Modifier.pointerInput(items.size, columns) {
        detectDragGesturesAfterLongPress(
            onDragStart = { start ->
                val info = itemAt(start)
                if (info == null) {
                    draggedKey = null
                    return@detectDragGesturesAfterLongPress
                }
                draggedKey = items.getOrNull(info.index)?.key
                pointerStart = start
                pointerNow = start
                anchorOffset = IntOffset(info.offset.x, info.offset.y)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDrag = { change, amount ->
                change.consume()
                val key = draggedKey ?: return@detectDragGesturesAfterLongPress
                pointerNow += amount

                val from = items.indexOfFirst { it.key == key }
                val target = itemAt(change.position)?.index ?: return@detectDragGesturesAfterLongPress
                if (from >= 0 && target != from && target in items.indices) {
                    items.add(target, items.removeAt(from))
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onDragEnd = {
                if (draggedKey != null) onReorder(items.toList())
                draggedKey = null
            },
            onDragCancel = { draggedKey = null },
        )
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(if (columns >= 5) 12.dp else 20.dp),
        userScrollEnabled = !editing,
        modifier = modifier
            .fillMaxSize()
            .then(dragModifier),
    ) {
        itemsIndexed(items, key = { _, entry -> entry.key }) { index, entry ->
            val dragging = entry.key == draggedKey
            // Сдвиг считается от пальца с поправкой на то, куда уехала сама ячейка.
            val liveOffset = if (dragging) offsetOf(entry.key) else null
            val translation = if (dragging && liveOffset != null) {
                Offset(
                    x = pointerNow.x - pointerStart.x + (anchorOffset.x - liveOffset.x),
                    y = pointerNow.y - pointerStart.y + (anchorOffset.y - liveOffset.y),
                )
            } else {
                Offset.Zero
            }
            AppCell(
                entry = entry,
                repository = repository,
                iconSize = iconSize,
                iconShape = iconShape,
                showLabel = showLabels,
                wobbling = editing && !dragging,
                // В режиме правки ячейка не слушает нажатия: иначе она съедает
                // долгий тап и до жеста перетаскивания дело не доходит.
                interactive = !editing,
                onClick = { onClick(entry) },
                onLongClick = { onLongClick(entry) },
                modifier = Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        if (dragging) {
                            translationX = translation.x
                            translationY = translation.y
                            scaleX = 1.1f
                            scaleY = 1.1f
                            alpha = 0.95f
                        }
                    }
                    // Перестановка анимируется только в режиме правки: на обычной
                    // прокрутке анимация позиции стоит лишних кадров.
                    .then(if (editing) Modifier.animateItem() else Modifier),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(
    entry: AppEntry,
    repository: AppRepository,
    iconSize: Dp,
    iconShape: app.plein.ui.icons.IconShape,
    showLabel: Boolean,
    wobbling: Boolean,
    interactive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wobble by animateFloatAsState(if (wobbling) 1f else 0f, label = "wobble")


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = if (wobble > 0f) 2f * wobble else 0f }
            // Радиус мелкий: подпись стоит у нижнего края и попадала под клип.
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (interactive) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier
                }
            )
            .padding(bottom = 6.dp),
    ) {
        AppIcon(entry = entry, repository = repository, size = iconSize, iconShape = iconShape)
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.width(iconSize + 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = entry.title,
                    fontSize = 10.5.sp,
                    lineHeight = 12.sp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
