package app.plein.layout

import app.plein.data.CellItem
import app.plein.data.CellLayout
import app.plein.data.Cell
import app.plein.data.Placement
import app.plein.data.RingFolder
import app.plein.data.RingFolders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Круговая папка: кольцо, попадание пальца и место в раскладке.
 *
 * Считать глазами тут нечего — значки стоят под углом, а касание разбирается
 * арифметикой. Числа держит тест, чтобы правка отрисовки не сдвинула кольцо
 * молча.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RingFolderTest {

    private fun folder(count: Int) = RingFolder(
        id = "r1",
        title = "Дом",
        appKeys = (1..count).map { "app$it" },
    )

    @Test
    fun `в кольце стоят первые шесть, остальные за серединой`() {
        val folder = folder(13)
        assertEquals(6, folder.ring().size)
        assertEquals(listOf("app1", "app2", "app3", "app4", "app5", "app6"), folder.ring())
        assertEquals(7, folder.rest())
    }

    @Test
    fun `неполная папка не обещает остатка`() {
        assertEquals(0, folder(4).rest())
        assertEquals(4, folder(4).ring().size)
    }

    @Test
    fun `первое место стоит сверху`() {
        val (x, y) = RingFolders.offsetOf(0, 6)
        assertEquals(0f, x, 0.001f)
        assertEquals(-RingFolders.RADIUS, y, 0.001f)
    }

    @Test
    fun `места идут по часовой стрелке`() {
        // Четверть круга при шести местах — это полтора шага, поэтому берём
        // четвёртое место: оно обязано стоять ровно снизу.
        val (x, y) = RingFolders.offsetOf(3, 6)
        assertEquals(0f, x, 0.001f)
        assertEquals(RingFolders.RADIUS, y, 0.001f)

        // Второе место уходит вправо и вверх.
        val (x2, y2) = RingFolders.offsetOf(1, 6)
        assertTrue("второе место должно уйти вправо: $x2", x2 > 0f)
        assertTrue("второе место должно остаться выше середины: $y2", y2 < 0f)
    }

    @Test
    fun `значки не залезают на середину и не выходят за край`() {
        val edge = RingFolders.RADIUS + RingFolders.ICON / 2
        assertTrue("значок вылезает за диск: $edge", edge < 0.5f)
        val gap = RingFolders.RADIUS - RingFolders.ICON / 2 - RingFolders.CORE / 2
        assertTrue("значок налезает на середину: $gap", gap > 0f)
    }

    @Test
    fun `папка переживает запись и чтение раскладки`() {
        val placements = listOf(
            Placement(CellItem.Ring("r1"), Cell(row = 1, col = 2, width = 2, height = 2)),
            Placement(CellItem.Tile("clock"), Cell(row = 0, col = 0, width = 2, height = 2)),
        )
        val back = CellLayout.decode(CellLayout.encode(placements))
        assertEquals(2, back.size)
        val ring = back.first { it.item is CellItem.Ring }
        assertEquals(CellItem.Ring("r1"), ring.item)
        assertEquals(Cell(row = 1, col = 2, width = 2, height = 2), ring.cell)
    }
}
