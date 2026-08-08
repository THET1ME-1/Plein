package app.plein.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Разбор строки — то место, где ошибка тише всего и заметна позже всего. */
class ConverterTest {

    @Test
    fun `километры в мили`() {
        val result = Converter.convert("10 км в мили")
        assertEquals(6.21, result!!.value, 0.01)
        assertEquals("мили", result.unit)
    }

    @Test
    fun `английская запись`() {
        val result = Converter.convert("5 kg to lb")
        assertEquals(11.02, result!!.value, 0.01)
    }

    @Test
    fun `фаренгейт в цельсий`() {
        val result = Converter.convert("100 f в c")
        assertEquals(37.78, result!!.value, 0.01)
        assertEquals("°C", result.unit)
    }

    @Test
    fun `цельсий в кельвин`() {
        val result = Converter.convert("0 c to k")
        assertEquals(273.15, result!!.value, 0.01)
    }

    @Test
    fun `запятая как разделитель дроби`() {
        val result = Converter.convert("1,5 часа в минуты")
        assertEquals(90.0, result!!.value, 0.01)
    }

    @Test
    fun `гигабайты в мегабайты`() {
        val result = Converter.convert("2 гб в мб")
        assertEquals(2048.0, result!!.value, 0.01)
    }

    @Test
    fun `разные семейства не переводятся`() {
        assertNull(Converter.convert("10 км в кг"))
    }

    @Test
    fun `без разделителя это не перевод`() {
        assertNull(Converter.convert("10 км"))
    }

    @Test
    fun `число печатается без хвоста нулей`() {
        assertEquals("90", Converter.format(90.0))
        assertEquals("6.21", Converter.format(6.2137))
        assertEquals("0.0254", Converter.format(0.0254))
    }
}
