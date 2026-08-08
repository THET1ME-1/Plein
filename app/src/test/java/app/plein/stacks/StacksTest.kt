package app.plein.stacks

import app.plein.data.Stacks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Стопки редких приложений.
 *
 * Главное правило: частое не прячем. Человек ставил эти значки руками, и
 * лаунчер не вправе решать за него, что убрать с глаз.
 */
class StacksTest {

    private val launches = mapOf(
        "часто" to 100, "тоже" to 60, "иногда" to 30,
        "редко1" to 2, "редко2" to 1, "редко3" to 0, "редко4" to 3, "редко5" to 1,
    )

    private fun of(key: String) = launches[key] ?: 0

    @Test
    fun `выключено — ничего не трогаем`() {
        val (singles, groups) = Stacks.split(launches.keys.toList(), ::of, enabled = false)
        assertEquals(launches.size, singles.size)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `частые остаются на виду`() {
        val (singles, _) = Stacks.split(launches.keys.toList(), ::of, enabled = true)
        assertTrue("частое спрятали", singles.containsAll(listOf("часто", "тоже", "иногда")))
    }

    @Test
    fun `редкие уходят в стопку по четыре`() {
        val (_, groups) = Stacks.split(launches.keys.toList(), ::of, enabled = true)
        assertEquals(1, groups.size)
        assertEquals(Stacks.SIZE, groups.first().keys.size)
    }

    @Test
    fun `остаток редких остаётся значками`() {
        val (singles, groups) = Stacks.split(launches.keys.toList(), ::of, enabled = true)
        val inGroups = groups.flatMap { it.keys }
        val all = singles + inGroups
        assertEquals("приложения потерялись", launches.size, all.size)
    }

    @Test
    fun `маленький список не трогаем`() {
        val few = listOf("a", "b", "c")
        val (singles, groups) = Stacks.split(few, { 0 }, enabled = true)
        assertEquals(few, singles)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `без статистики ничего не прячем`() {
        val keys = List(10) { "app$it" }
        val (singles, groups) = Stacks.split(keys, { 0 }, enabled = true)
        assertEquals(keys.size, singles.size)
        assertTrue(groups.isEmpty())
    }
}
