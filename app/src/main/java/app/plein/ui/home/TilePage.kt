package app.plein.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.roundToInt

/**
 * Страница с плитками.
 *
 * Иконки и плитки живут в одной сетке: плитка стоит на своих клетках, иконки
 * обтекают её. В режиме правки плитку берут долгим нажатием и переносят по
 * клеткам — с прилипанием, чтобы не целиться в пиксели.
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
    onClick: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
    onTileMenu: (CellItem) -> Unit,
    onTileMove: (CellItem, Cell) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val placements = remember(apps, tiles, columns) {
        CellLayout.build(apps.map { it.key }, tiles, columns)
    }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val haptics = rememberHaptics()
    val density = LocalDensity.current

    var dragging by remember { mutableStateOf<CellItem?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = !editing)
    ) {
        CellGrid(
            placements = placements,
            columns = columns,
            rowHeight = rowHeight,
            modifier = Modifier.fillMaxWidth(),
        ) { placement ->
            when (val item = placement.item) {
                is CellItem.App -> {
                    val entry = byKey[item.key]
                    if (entry != null) {
                        AppCellItem(
                            entry = entry,
                            repository = repository,
                            iconSize = iconSize,
                            iconShape = iconShape,
                            iconPack = iconPack,
                            monoMode = monoMode,
                            showLabel = showLabels,
                            interactive = !editing,
                            onClick = { onClick(entry) },
                            onLongClick = {
                                haptics.longPress()
                                onLongClick(entry)
                            },
                        )
                    }
                }

                is CellItem.Tile -> {
                    val active = dragging?.id == item.id
                    Box(
                        Modifier
                            .fillMaxSize()
                            .zIndex(if (active) 2f else 0f)
                            .graphicsLayer {
                                if (active) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    scaleX = 1.04f
                                    scaleY = 1.04f
                                }
                            }
                            .then(
                                if (!editing) Modifier else Modifier.pointerInput(item.id, columns) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragging = item
                                            dragOffset = Offset.Zero
                                            haptics.longPress()
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                        },
                                        onDragEnd = {
                                            val cellWidth = with(density) {
                                                (size.width.toFloat() / placement.cell.width)
                                            }
                                            val cellHeight = with(density) { rowHeight.toPx() }
                                            val movedCols = (dragOffset.x / cellWidth).roundToInt()
                                            val movedRows = (dragOffset.y / cellHeight).roundToInt()
                                            val target = placement.cell.copy(
                                                row = placement.cell.row + movedRows,
                                                col = placement.cell.col + movedCols,
                                            )
                                            val moved = onTileMove(item, target)
                                            if (moved) haptics.confirm() else haptics.reject()
                                            dragging = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            dragging = null
                                            dragOffset = Offset.Zero
                                        },
                                    )
                                }
                            )
                            .then(
                                if (!editing) Modifier
                                else Modifier.combinedClickable(onClick = { onTileMenu(item) })
                            )
                    ) {
                        tileContent(item.kind)
                        if (editing) {
                            // В правке плитка чуть светлеет: видно, что её можно взять.
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .alpha(0.12f)
                            )
                        }
                    }
                }
            }
        }
    }
}
