package app.plein.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.plein.data.Cell
import app.plein.data.Placement

/**
 * Ячеистая сетка страницы.
 *
 * `LazyVerticalGrid` растягивает элемент только по ширине, поэтому плитка в
 * две строки с иконками сбоку на нём не собирается.
 *
 * Позиция каждого элемента анимируется пружиной: когда плитку тащат, соседние
 * иконки разъезжаются на глазах, а не перепрыгивают. Без этого непонятно, куда
 * плитка встанет и что подвинется.
 */
@Composable
fun CellGrid(
    placements: List<Placement>,
    columns: Int,
    rowHeight: Dp,
    horizontalPadding: Dp = 20.dp,
    highlight: Cell? = null,
    highlightInsets: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(4.dp),
    modifier: Modifier = Modifier,
    content: @Composable (Placement) -> Unit,
) {
    val rows = placements.maxOfOrNull { it.cell.lastRow + 1 } ?: 0

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(rowHeight * rows.coerceAtLeast(1))
    ) {
        val cellWidth = (maxWidth - horizontalPadding * 2) / columns

        // Место, куда встанет плитка: рисуем под элементами, чтобы призрак не
        // перекрывал то, что человек тащит.
        highlight?.let { cell ->
            Box(
                Modifier
                    .offset(
                        x = horizontalPadding + cellWidth * cell.col,
                        y = rowHeight * cell.row,
                    )
                    .size(cellWidth * cell.width, rowHeight * cell.height)
                    .padding(highlightInsets)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            )
        }

        placements.forEach { placement ->
            key(placement.item.id) {
                val x by animateDpAsState(
                    targetValue = horizontalPadding + cellWidth * placement.cell.col,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                    label = "x",
                )
                val y by animateDpAsState(
                    targetValue = rowHeight * placement.cell.row,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                    label = "y",
                )
                Box(
                    Modifier
                        .offset(x = x, y = y)
                        .size(cellWidth * placement.cell.width, rowHeight * placement.cell.height)
                ) {
                    content(placement)
                }
            }
        }
    }
}
