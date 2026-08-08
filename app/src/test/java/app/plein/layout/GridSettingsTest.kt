package app.plein.layout

import app.plein.data.Cell
import app.plein.data.CellItem
import app.plein.data.CellLayout
import app.plein.data.Placement
import app.plein.data.TileSizes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Настройки сетки и раскладка.
 *
 * Число колонок, размер значка и подписи человек меняет на ходу, и раскладка
 * обязана это переживать: плитка не уезжает за край, значок не режется, а
 * страница не рассыпается на пустые клетки.
 */
class GridSettingsTest {

    private val tiles = listOf(
        Placement(CellItem.Tile("clock"), Cell(row = 0, col = 2, width = 2, height = 2)),
        Placement(CellItem.Tile("note"), Cell(row = 3, col = 0, width = 4, height = 2)),
    )

    @Test
    fun `на любой сетке плитки внутри экрана`() {
        listOf(3, 4, 5, 6).forEach { columns ->
            val placed = CellLayout.build(List(20) { "app$it" }, tiles, columns)
            placed.forEach { placement ->
                assertTrue(
                    "на $columns колонках элемент вылез: ${placement.cell}",
                    placement.cell.withinColumns(columns),
                )
            }
        }
    }

    @Test
    fun `все приложения нашли место`() {
        listOf(3, 4, 5, 6).forEach { columns ->
            val apps = List(37) { "app$it" }
            val placed = CellLayout.build(apps, tiles, columns)
            assertEquals(
                "на $columns колонках потерялись приложения",
                apps.size,
                placed.count { it.item is CellItem.App },
            )
        }
    }

    @Test
    fun `размеры плиток укладываются в самую узкую сетку`() {
        TileSizes.variants("clock").forEach { (width, _) ->
            assertTrue("плитка шире трёх колонок: $width", width <= 4)
        }
    }
}
