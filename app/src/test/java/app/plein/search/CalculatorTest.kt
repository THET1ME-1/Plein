package app.plein.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {

    @Test
    fun `умножение считается`() {
        assertEquals(84.0, Calculator.evaluate("12*7")!!, 0.0001)
    }

    @Test
    fun `приоритет операций соблюдается`() {
        assertEquals(6.0, Calculator.evaluate("2+2*2")!!, 0.0001)
    }

    @Test
    fun `скобки меняют порядок`() {
        assertEquals(8.0, Calculator.evaluate("(2+2)*2")!!, 0.0001)
    }

    @Test
    fun `процент делит на сто`() {
        assertEquals(0.15, Calculator.evaluate("15%")!!, 0.0001)
    }

    @Test
    fun `запятая работает как точка`() {
        assertEquals(3.5, Calculator.evaluate("1,5+2")!!, 0.0001)
    }

    @Test
    fun `знак умножения из клавиатуры`() {
        assertEquals(12.0, Calculator.evaluate("3 × 4")!!, 0.0001)
    }

    @Test
    fun `название приложения не считается выражением`() {
        assertNull(Calculator.evaluate("Telegram"))
    }

    @Test
    fun `голое число не считается выражением`() {
        assertNull(Calculator.evaluate("42"))
    }

    @Test
    fun `битое выражение не роняет разбор`() {
        assertNull(Calculator.evaluate("2+"))
    }

    @Test
    fun `деление на ноль не выдаёт результат`() {
        assertNull(Calculator.evaluate("5/0"))
    }

    @Test
    fun `целые печатаются без хвоста`() {
        assertEquals("84", Calculator.format(84.0))
    }

    @Test
    fun `дробные сохраняют точность`() {
        assertEquals("3.5", Calculator.format(3.5))
    }
}
