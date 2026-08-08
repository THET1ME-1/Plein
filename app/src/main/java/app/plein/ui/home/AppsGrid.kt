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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
    shape: Shape,
    showLabels: Boolean,
    editing: Boolean,
    onReorder: (List<AppEntry>) -> Unit,
    onClick: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(apps) { apps.toMutableStateList() }
    val gridState = rememberLazyGridState()

    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Выход из режима правки фиксирует порядок один раз, а не на каждый сдвиг.
    LaunchedEffect(editing) {
        if (!editing && dragIndex >= 0) dragIndex = -1
    }

    fun itemAt(position: Offset): LazyGridItemInfo? =
        gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
            position.x >= info.offset.x && position.x <= info.offset.x + info.size.width &&
                position.y >= info.offset.y && position.y <= info.offset.y + info.size.height
        }

    val dragModifier = if (!editing) Modifier else Modifier.pointerInput(items.size, columns) {
        detectDragGesturesAfterLongPress(
            onDragStart = { start ->
                dragIndex = itemAt(start)?.index ?: -1
                dragOffset = Offset.Zero
            },
            onDrag = { change, amount ->
                change.consume()
                if (dragIndex < 0) return@detectDragGesturesAfterLongPress
                dragOffset += amount
                val target = itemAt(change.position)?.index ?: return@detectDragGesturesAfterLongPress
                if (target != dragIndex && target in items.indices) {
                    items.add(target, items.removeAt(dragIndex))
                    dragIndex = target
                    dragOffset = Offset.Zero
                }
            },
            onDragEnd = {
                if (dragIndex >= 0) onReorder(items.toList())
                dragIndex = -1
                dragOffset = Offset.Zero
            },
            onDragCancel = {
                dragIndex = -1
                dragOffset = Offset.Zero
            },
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
            val dragging = index == dragIndex
            AppCell(
                entry = entry,
                repository = repository,
                iconSize = iconSize,
                shape = shape,
                showLabel = showLabels,
                wobbling = editing && !dragging,
                onClick = { if (!editing) onClick(entry) },
                onLongClick = { if (!editing) onLongClick(entry) },
                modifier = Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        if (dragging) {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                            scaleX = 1.12f
                            scaleY = 1.12f
                            alpha = 0.92f
                        }
                    }
                    .animateItem(),
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
    shape: Shape,
    showLabel: Boolean,
    wobbling: Boolean,
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(bottom = 6.dp),
    ) {
        AppIcon(entry = entry, repository = repository, size = iconSize, shape = shape)
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
