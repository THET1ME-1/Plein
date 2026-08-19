package app.plein.layout

import app.plein.data.Cell
import app.plein.data.CellItem
import app.plein.data.CellLayout
import app.plein.data.Placement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Ячеистая раскладка страницы.
 *
 * Здесь живёт вся арифметика домашнего экрана: плитка занимает несколько
 * клеток, приложения обтекают её, а при смене числа колонок ничего не должно
 * пропасть за краем.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CellLayoutTest {

    private val clock = Placement(CellItem.Tile("clock"), Cell(row = 1, col = 0, width = 2, height = 2))

    @Test
    fun `приложения обтекают плитку`() {
        val apps = List(12) { "app$it" }
        val placed = CellLayout.build(apps, listOf(clock), columns = 4)

        // Первый ряд целиком под приложения.
        val firstRow = placed.filter { it.cell.row == 0 }
        assertEquals(4, firstRow.size)

        // Рядом с плиткой во втором ряду остаются две клетки.
        val besideTile = placed.filter { it.cell.row == 1 && it.item is CellItem.App }
        assertEquals(2, besideTile.size)
        assertTrue(besideTile.all { it.cell.col >= 2 })
    }

    @Test
    fun `плитка не пересекается ни с чем`() {
        val placed = CellLayout.build(List(20) { "app$it" }, listOf(clock), columns = 4)
        val tile = placed.first { it.item is CellItem.Tile }.cell
        val others = placed.filter { it.item is CellItem.App }.map { it.cell }
        assertTrue("плитка накрыла приложение", others.none { it.overlaps(tile) })
    }

    @Test
    fun `широкая плитка ужимается под узкую сетку`() {
        val wide = Placement(CellItem.Tile("weather"), Cell(row = 0, col = 0, width = 4, height = 2))
        val placed = CellLayout.build(List(6) { "app$it" }, listOf(wide), columns = 3)
        val tile = placed.first { it.item is CellItem.Tile }.cell

        assertTrue("плитка вылезла за край: ${tile.lastCol}", tile.withinColumns(3))
        assertEquals(3, tile.width)
    }

    @Test
    fun `две плитки не наезжают друг на друга`() {
        val second = Placement(CellItem.Tile("weather"), Cell(row = 1, col = 1, width = 2, height = 2))
        val placed = CellLayout.build(emptyList(), listOf(clock, second), columns = 4)
        val cells = placed.map { it.cell }
        assertFalse("плитки пересеклись", cells[0].overlaps(cells[1]))
    }

    @Test
    fun `высота страницы считается по нижней клетке`() {
        val placed = CellLayout.build(List(4) { "app$it" }, listOf(clock), columns = 4)
        assertEquals(3, CellLayout.rowsOf(placed))
    }

    @Test
    fun `перенос на занятое место не разрешается`() {
        val placed = CellLayout.build(List(8) { "app$it" }, listOf(clock), columns = 4)
        val moving = placed.first { it.item is CellItem.Tile }.item

        assertFalse(CellLayout.canPlace(placed, moving, Cell(0, 0, 2, 2), columns = 4))
        assertTrue(CellLayout.canPlace(placed, moving, Cell(5, 0, 2, 2), columns = 4))
    }

    @Test
    fun `за край сетки не поставить`() {
        val placed = CellLayout.build(emptyList(), listOf(clock), columns = 4)
        val moving = placed.first().item
        assertFalse(CellLayout.canPlace(placed, moving, Cell(0, 3, 2, 2), columns = 4))
        assertFalse(CellLayout.canPlace(placed, moving, Cell(-1, 0, 2, 2), columns = 4))
    }

    @Test
    fun `раскладка переживает запись и чтение`() {
        val tiles = listOf(clock, Placement(CellItem.Tile("battery"), Cell(4, 2, 2, 1)))
        val restored = CellLayout.decode(CellLayout.encode(tiles))
        assertEquals(tiles, restored)
    }

    @Test
    fun `размер меняется на месте, когда он влезает`() {
        val media = Placement(CellItem.Tile("media"), Cell(row = 0, col = 0, width = 4, height = 2))
        val cell = CellLayout.fitResize(listOf(media), media.item, width = 2, height = 2, columns = 4)

        assertEquals(Cell(row = 0, col = 0, width = 2, height = 2), cell)
    }

    @Test
    fun `плитка съезжает влево, когда справа не хватает места`() {
        val media = Placement(CellItem.Tile("media"), Cell(row = 0, col = 2, width = 2, height = 2))
        val cell = CellLayout.fitResize(listOf(media), media.item, width = 4, height = 2, columns = 4)

        assertEquals(Cell(row = 0, col = 0, width = 4, height = 2), cell)
    }

    @Test
    fun `занятая строка уводит плитку в свободную`() {
        val media = Placement(CellItem.Tile("media"), Cell(row = 0, col = 0, width = 2, height = 2))
        val note = Placement(CellItem.Tile("note"), Cell(row = 0, col = 2, width = 2, height = 2))
        val cell = CellLayout.fitResize(listOf(media, note), media.item, width = 4, height = 2, columns = 4)

        assertEquals(4, cell?.width)
        assertTrue("плитка налезла на соседку", cell?.overlaps(note.cell) == false)
    }

    @Test
    fun `размер шире сетки не берётся`() {
        val media = Placement(CellItem.Tile("media"), Cell(row = 0, col = 0, width = 2, height = 2))
        val cell = CellLayout.fitResize(listOf(media), media.item, width = 4, height = 2, columns = 3)

        assertEquals(null, cell)
    }
}
