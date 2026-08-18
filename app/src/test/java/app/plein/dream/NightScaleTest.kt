package app.plein.dream

import app.plein.ui.dream.NightScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Полоса ночи под часами: сколько осталось до будильника.
 *
 * Отсчёт идёт от минуты, когда телефон поставили на зарядку, а не от
 * условных десяти вечера: лёг в два ночи — полоса начинается с нуля.
 */
class NightScaleTest {

    private val minute = 60_000L
    private val started = 0L

    @Test
    fun `без будильника полосы нет`() {
        assertNull(NightScale.of(started, now = 10 * minute, alarmAt = null))
    }

    @Test
    fun `далёкий будильник за ночь не считается`() {
        // Двадцать часов вперёд — это не сон, а завтрашние дела.
        assertNull(NightScale.of(started, now = 0, alarmAt = 20 * 60 * minute))
    }

    @Test
    fun `середина ночи это половина полосы`() {
        val scale = NightScale.of(started, now = 4 * 60 * minute, alarmAt = 8 * 60 * minute)!!
        assertEquals(0.5f, scale.progress, 0.001f)
        assertEquals(4 * 60, scale.minutesLeft)
    }

    @Test
    fun `после будильника полоса полная и остатка нет`() {
        val scale = NightScale.of(started, now = 9 * 60 * minute, alarmAt = 8 * 60 * minute)!!
        assertEquals(1f, scale.progress, 0.001f)
        assertEquals(0, scale.minutesLeft)
    }

    @Test
    fun `будильник в ту же минуту полосы не даёт`() {
        assertNull(NightScale.of(started, now = started, alarmAt = started))
    }

    @Test
    fun `остаток округляется вверх, чтобы не показывать ноль раньше времени`() {
        val scale = NightScale.of(started, now = 30_000L, alarmAt = 60 * minute)!!
        assertEquals(60, scale.minutesLeft)
    }
}
