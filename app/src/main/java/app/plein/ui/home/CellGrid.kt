package app.plein.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.plein.data.Placement
import kotlin.math.roundToInt

/**
 * Ячеистая сетка страницы.
 *
 * `LazyVerticalGrid` умеет растягивать элемент по ширине, но не по высоте,
 * поэтому плитка в две строки с иконками сбоку на нём не собирается. Здесь
 * место каждого элемента задано клетками, и раскладка считается арифметикой:
 * ширина клетки — доля ширины экрана, высота — заданная величина.
 *
 * Промежутки съедаются самими клетками: элемент занимает клетку целиком, а
 * отступ рисует уже его содержимое. Так плитка два на два выглядит цельной, а
 * не собранной из четырёх кусков с дырками.
 */
@Composable
fun CellGrid(
    placements: List<Placement>,
    columns: Int,
    rowHeight: Dp,
    horizontalPadding: Dp = 20.dp,
    modifier: Modifier = Modifier,
    content: @Composable (Placement) -> Unit,
) {
    Layout(
        content = {
            placements.forEach { placement ->
                Box(Modifier) { content(placement) }
            }
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val padding = horizontalPadding.roundToPx()
        val usable = (constraints.maxWidth - padding * 2).coerceAtLeast(0)
        val cellWidth = usable.toFloat() / columns
        val cellHeight = rowHeight.roundToPx()

        val rows = placements.maxOfOrNull { it.cell.lastRow + 1 } ?: 0
        val height = rows * cellHeight

        val placed = measurables.mapIndexed { index, measurable ->
            val cell = placements[index].cell
            val width = (cell.width * cellWidth).roundToInt()
            measurable.measure(
                Constraints.fixed(width = width, height = cell.height * cellHeight)
            )
        }

        layout(constraints.maxWidth, height) {
            placed.forEachIndexed { index, placeable ->
                val cell = placements[index].cell
                placeable.place(
                    x = padding + (cell.col * cellWidth).roundToInt(),
                    y = cell.row * cellHeight,
                )
            }
        }
    }
}
