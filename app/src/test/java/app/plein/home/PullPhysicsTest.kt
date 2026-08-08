package app.plein.home

import app.plein.ui.home.PullPhysics
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Оттягивание за новым кадром.
 *
 * Прошлая резина гасила прирост квадратом, и ход подходил к пределу, но не
 * доходил: порог не срабатывал никогда, кадр не приезжал. Тест держит два
 * условия разом — дотянуть можно, но не случайным движением.
 */
class PullPhysicsTest {

    private val density = 3f
    private val limit = 130f * density

    @Test
    fun `порог берётся одним движением`() {
        val travelDp = PullPhysics.travelToTrigger(limit) / density
        // Треть экрана — уверенный жест. Больше половины уже не пройти рукой.
        assertTrue("палец должен пройти меньше 320 dp, а проходит $travelDp", travelDp < 320f)
        assertTrue("слишком легко: $travelDp", travelDp > 150f)
    }

    @Test
    fun `случайным движением не сработает`() {
        var value = 0f
        repeat(20) { value = PullPhysics.accumulate(value, 12f, limit) }
        assertTrue("сработало от короткого движения", value < limit * PullPhysics.TRIGGER)
    }

    @Test
    fun `резина не глохнет у края`() {
        assertTrue(PullPhysics.resistance(1f) > 0.1f)
        assertTrue(PullPhysics.resistance(0f) > PullPhysics.resistance(1f))
    }
}
