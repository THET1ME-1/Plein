package app.plein.adaptive

import app.plein.ui.home.Adaptive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Сетка под ширину экрана: телефон, раскладушка, планшет. */
class AdaptiveTest {

    @Test
    fun `телефон оставляет выбор человека`() {
        assertEquals(4, Adaptive.columnsFor(widthDp = 411, chosen = 4))
        assertEquals(5, Adaptive.columnsFor(widthDp = 393, chosen = 5))
    }

    @Test
    fun `раскладушка и планшет добавляют колонки`() {
        assertTrue(Adaptive.columnsFor(widthDp = 673, chosen = 4) > 4)
        assertTrue(Adaptive.columnsFor(widthDp = 800, chosen = 4) >= 7)
        assertTrue(Adaptive.columnsFor(widthDp = 1200, chosen = 4) >= 8)
    }

    @Test
    fun `колонок не бывает меньше трёх и больше дюжины`() {
        assertEquals(3, Adaptive.columnsFor(widthDp = 320, chosen = 1))
        assertEquals(12, Adaptive.columnsFor(widthDp = 1600, chosen = 10))
    }

    @Test
    fun `широкий экран получает поля и две колонки`() {
        assertEquals(0, Adaptive.sheetPadding(411).value.toInt())
        assertTrue(Adaptive.sheetPadding(1000).value > 0)
        assertFalse(Adaptive.twoColumns(411))
        assertTrue(Adaptive.twoColumns(800))
    }
}
