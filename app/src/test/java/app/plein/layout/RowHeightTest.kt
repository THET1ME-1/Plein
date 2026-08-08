package app.plein.layout

import app.plein.ui.home.CellMetrics
import app.plein.ui.home.iconSizeFor
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Клетка обязана вмещать значок.
 *
 * Высота ряда стояла константой, а размер значка зависит от числа колонок: на
 * трёх колонках значок 82 dp, и вместе с подписью он в клетку 96 dp не влезал
 * — низ срезало.
 */
class RowHeightTest {

    @Test
    fun `клетка вмещает значок с подписью`() {
        listOf(3, 4, 5, 6).forEach { columns ->
            val icon = iconSizeFor(columns).value
            val row = CellMetrics.rowHeightFor(columns, showLabels = true).value
            assertTrue(
                "на $columns колонках клетка $row меньше значка $icon с подписью",
                row >= icon + CellMetrics.LABEL_SPACE,
            )
        }
    }

    @Test
    fun `без подписей клетка ниже`() {
        listOf(3, 4, 5, 6).forEach { columns ->
            val withLabel = CellMetrics.rowHeightFor(columns, showLabels = true).value
            val without = CellMetrics.rowHeightFor(columns, showLabels = false).value
            assertTrue("подписи не изменили высоту на $columns", without < withLabel)
            assertTrue("без подписи значок не влезает", without >= iconSizeFor(columns).value)
        }
    }

    @Test
    fun `своя высота уважается, но не режет значок`() {
        // Человек поставил 84 — на трёх колонках этого мало, поднимаем до нужного.
        val chosen = CellMetrics.resolve(custom = 84, columns = 3, showLabels = true).value
        assertTrue("своя высота срезала значок: $chosen", chosen >= iconSizeFor(3).value)

        // На шести колонках 84 с запасом, оставляем как выбрано.
        val roomy = CellMetrics.resolve(custom = 84, columns = 6, showLabels = true).value
        assertTrue(roomy == 84f)
    }
}
