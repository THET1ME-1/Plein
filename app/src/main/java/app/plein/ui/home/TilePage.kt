package app.plein.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.Cell
import app.plein.data.CellItem
import app.plein.data.CellLayout
import app.plein.data.MonoMode
import app.plein.data.Placement
import app.plein.ui.icons.IconShape
import app.plein.ui.rememberHaptics
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/**
 * Страница с плитками.
 *
 * Пока плитку тащат, раскладка пересчитывается на каждой смене клетки: иконки
 * разъезжаются под пальцем, а место будущей плитки подсвечено. Без этого было
 * непонятно, куда она встанет и кого подвинет.
 */
@Composable
fun TilePage(
    apps: List<AppEntry>,
    tiles: List<Placement>,
    repository: AppRepository,
    columns: Int,
    rowHeight: Dp,
    iconSize: Dp,
    iconShape: IconShape,
    iconPack: String,
    monoMode: MonoMode,
    showLabels: Boolean,
    editing: Boolean,
    tileContent: @Composable (String) -> Unit,
    widgetContent: @Composable (Int, Int, Int) -> Unit,
    onClick: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
    onTileMenu: (CellItem) -> Unit,
    onTileRemove: (CellItem) -> Unit,
    onReorder: (List<String>) -> Unit,
    onTileAction: (String) -> Unit,
    onTileMove: (CellItem, Cell) -> Boolean,
    onStartEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val density = LocalDensity.current

    var dragging by remember { mutableStateOf<CellItem?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // Клетка, куда плитка встанет прямо сейчас. Пока тащат, раскладка живёт с
    // ней, а не с исходной — поэтому соседи и разъезжаются.
    var previewCell by remember { mutableStateOf<Cell?>(null) }
    val editingState = rememberUpdatedState(editing)

    val liveTiles = remember(tiles, dragging, previewCell) {
        val target = previewCell
        val moved = dragging
        if (target == null || moved == null) tiles
        else tiles.map { if (it.item.id == moved.id) it.copy(cell = target) else it }
    }

    val placements = remember(apps, liveTiles, columns) {
        CellLayout.build(apps.map { it.key }, liveTiles, columns)
    }
    val byKey = remember(apps) { apps.associateBy { it.key } }

    val cellHeightPx = with(density) { rowHeight.toPx() }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Шаг перетаскивания меряем той же линейкой, что рисует сетку. Пока
        // ширина бралась из `LocalConfiguration`, любой отступ страницы уводил
        // плитку мимо клетки — считалось по экрану, рисовалось по контейнеру.
        val cellWidthPx = with(density) { ((maxWidth - 40.dp) / columns).toPx() }

        CellGrid(
            placements = placements,
            columns = columns,
            rowHeight = rowHeight,
            highlight = previewCell,
            modifier = Modifier.fillMaxWidth(),
        ) { placement ->
            when (val item = placement.item) {
                is CellItem.App -> {
                    val entry = byKey[item.key]
                    if (entry != null) {
                        val active = dragging?.id == item.id
                        val cellState = rememberUpdatedState(placement.cell)
                        Box(
                            Modifier
                                .fillMaxSize()
                                .zIndex(if (active) 3f else 0f)
                                .graphicsLayer {
                                    if (active) {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                        scaleX = 1.08f
                                        scaleY = 1.08f
                                        alpha = 0.94f
                                    }
                                }
                                // Иконка переносится тем же жестом, что и плитка:
                                // на странице с плитками своя раскладка, и
                                // прежнее перетаскивание из ленивой сетки сюда
                                // не доставало — значки стояли намертво.
                                .pointerInput(item.id, columns) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val from = cellState.value
                                        var held = false
                                        var travelled = Offset.Zero

                                        try {
                                            val slipped = withTimeoutOrNull(
                                                viewConfiguration.longPressTimeoutMillis,
                                            ) {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes
                                                        .firstOrNull { it.id == down.id }
                                                        ?: return@withTimeoutOrNull true
                                                    if (!change.pressed) return@withTimeoutOrNull true
                                                    travelled = change.position - down.position
                                                    if (travelled.getDistance() > viewConfiguration.touchSlop) {
                                                        return@withTimeoutOrNull true
                                                    }
                                                }
                                                @Suppress("UNREACHABLE_CODE")
                                                false
                                            }

                                            if (slipped == null) {
                                                held = true
                                                dragging = item
                                                dragOffset = Offset.Zero
                                                previewCell = from
                                                haptics.longPress()
                                            }

                                            while (held) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes
                                                    .firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) break
                                                change.consume()
                                                dragOffset += change.positionChange()
                                                val cols = (dragOffset.x / cellWidthPx).roundToInt()
                                                val rows = (dragOffset.y / cellHeightPx).roundToInt()
                                                val wanted = from.copy(
                                                    row = (from.row + rows).coerceAtLeast(0),
                                                    col = (from.col + cols).coerceIn(0, columns - 1),
                                                )
                                                if (wanted != previewCell) {
                                                    previewCell = wanted
                                                    haptics.tick()
                                                }
                                            }

                                            if (held) {
                                                // Порядок задаётся списком: считаем,
                                                // на чьё место встал значок, и
                                                // переставляем его туда.
                                                val target = previewCell
                                                if (target != null) {
                                                    val keys = apps.map { it.key }.toMutableList()
                                                    val fromIndex = keys.indexOf(item.key)
                                                    val toIndex = placements
                                                        .filter { it.item is CellItem.App }
                                                        .indexOfFirst { it.cell.row == target.row && it.cell.col == target.col }
                                                    if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                                                        keys.add(toIndex, keys.removeAt(fromIndex))
                                                        onReorder(keys)
                                                        haptics.confirm()
                                                    }
                                                }
                                            } else if (travelled.getDistance() <= viewConfiguration.touchSlop) {
                                                if (editingState.value) onLongClick(entry) else onClick(entry)
                                            }
                                        } finally {
                                            if (dragging?.id == item.id) {
                                                dragging = null
                                                dragOffset = Offset.Zero
                                                previewCell = null
                                            }
                                        }
                                    }
                                }
                        ) {
                            AppCellItem(
                                entry = entry,
                                repository = repository,
                                iconSize = iconSize,
                                iconShape = iconShape,
                                iconPack = iconPack,
                                monoMode = monoMode,
                                showLabel = showLabels,
                                interactive = false,
                                onClick = {},
                                onLongClick = {},
                            )
                        }
                    }
                }

                is CellItem.Tile, is CellItem.Widget -> {
                    val active = dragging?.id == item.id
                    val lift by animateFloatAsState(
                        targetValue = if (active) 1.06f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                        label = "lift",
                    )
                    val startState = rememberUpdatedState(placement.cell)

                    Box(
                        Modifier
                            .fillMaxSize()
                            .zIndex(if (active) 3f else 0f)
                            .graphicsLayer {
                                if (active) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    alpha = 0.92f
                                }
                                scaleX = lift
                                scaleY = lift
                            }
                    ) {
                        when (item) {
                            is CellItem.Tile -> tileContent(item.kind)
                            is CellItem.Widget -> widgetContent(
                                item.widgetId,
                                placement.cell.width,
                                placement.cell.height,
                            )
                            else -> Unit
                        }

                        // Слой жестов поверх содержимого.
                        //
                        // Виджет приложения — чужая View, и касания она забирает
                        // себе: наш обработчик на родителе их не видел, поэтому
                        // виджет нельзя было ни взять, ни сдвинуть, ни даже
                        // открыть его меню. Ловим события раньше неё, но
                        // потребляем только когда человек удержал палец.
                        Box(
                            Modifier
                                .matchParentSize()
                                // Ключи без editing и без клетки: жест сам
                                // включает правку, и раньше это меняло ключ —
                                // pointerInput пересоздавался, а начатое
                                // перетаскивание умирало на первом же шаге.
                                // Свежие значения читаем через состояние.
                                .pointerInput(item.id, columns) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial,
                                        )
                                        
                                        var held = false
                                        var lifted = false
                                        var travelled = Offset.Zero

                                        try {
                                            // Удержание меряет ТАЙМЕР, а не поток
                                            // событий. Прежний код ждал очередного
                                            // события указателя, чтобы сверить время,
                                            // — но неподвижный палец событий не
                                            // порождает вовсе, и захват не наступал
                                            // никогда. Плитка бралась только если
                                            // палец мелко дрожал, да и то пока
                                            // накопленный путь не перевалит за порог.
                                            val start = startState.value
                                            val liveEditing = editingState.value
                                            val slipped = withTimeoutOrNull(
                                                viewConfiguration.longPressTimeoutMillis,
                                            ) {
                                                while (true) {
                                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                                    val change = event.changes
                                                        .firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                                                    if (!change.pressed) return@withTimeoutOrNull true
                                                    travelled = change.position - down.position
                                                    // Ушёл в сторону — это прокрутка
                                                    // страницы, не захват плитки.
                                                    if (travelled.getDistance() > viewConfiguration.touchSlop) {
                                                        return@withTimeoutOrNull true
                                                    }
                                                }
                                                @Suppress("UNREACHABLE_CODE")
                                                false
                                            }

                                            if (slipped == null) {
                                                // Таймер дошёл до конца, палец на
                                                // месте — плитка в руке.
                                                held = true
                                                lifted = true
                                                if (!liveEditing) onStartEditing()
                                                dragging = item
                                                dragOffset = Offset.Zero
                                                previewCell = start
                                                haptics.longPress()
                                            }

                                            while (held) {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                val change = event.changes
                                                    .firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) break
                                                change.consume()
                                                dragOffset += change.positionChange()
                                                val movedCols = (dragOffset.x / cellWidthPx).roundToInt()
                                                val movedRows = (dragOffset.y / cellHeightPx).roundToInt()
                                                val wanted = start.copy(
                                                    row = (start.row + movedRows).coerceAtLeast(0),
                                                    col = (start.col + movedCols)
                                                        .coerceIn(0, (columns - start.width).coerceAtLeast(0)),
                                                )
                                                if (wanted != previewCell) {
                                                    previewCell = wanted
                                                    haptics.tick()
                                                }
                                            }

                                            if (lifted) {
                                                val target = previewCell
                                                val done = target != null && onTileMove(item, target)
                                                if (done) haptics.confirm() else haptics.reject()
                                            } else if (liveEditing && travelled.getDistance() <= viewConfiguration.touchSlop) {
                                                // В правке короткое касание открывает
                                                // меню: размеры и «убрать».
                                                onTileMenu(item)
                                            } else if (!liveEditing &&
                                                travelled.getDistance() <= viewConfiguration.touchSlop &&
                                                item is CellItem.Tile
                                            ) {
                                                onTileAction(item.kind)
                                            }
                                        } finally {
                                            // Призрак места гасим при ЛЮБОМ исходе.
                                            // Пока уборка стояла только в удачной
                                            // ветке, оборванный жест оставлял на
                                            // экране висеть прямоугольник-подсветку
                                            // — ту самую «тень» рядом с плиткой.
                                            if (dragging?.id == item.id) {
                                                dragging = null
                                                dragOffset = Offset.Zero
                                                previewCell = null
                                            }
                                        }
                                    }
                                }
                        )

                        if (editing && !active) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            )
                        }

                        // Крестик в углу: удаление на виду, а не спрятано в меню.
                        if (editing) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable {
                                        haptics.confirm()
                                        onTileRemove(item)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
