package app.plein.theme

import app.plein.ui.theme.ThemeMode
import app.plein.ui.theme.isDark
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * Тёмная тема по расписанию.
 *
 * Час берётся из переданного момента, а не из системных часов на месте вызова:
 * иначе значение застывает на времени сборки экрана, лаунчер не замечает
 * наступления восьми вечера и не идёт за новым кадром.
 */
class ThemeScheduleTest {

    private fun at(hour: Int, minute: Int = 0): Date =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }.time

    @Test
    fun `день светлый`() {
        assertFalse(ThemeMode.AutoTime.isDark(systemDark = true, now = at(7)))
        assertFalse(ThemeMode.AutoTime.isDark(systemDark = true, now = at(12)))
        assertFalse(ThemeMode.AutoTime.isDark(systemDark = true, now = at(19, 59)))
    }

    @Test
    fun `ночь тёмная`() {
        assertTrue(ThemeMode.AutoTime.isDark(systemDark = false, now = at(20)))
        assertTrue(ThemeMode.AutoTime.isDark(systemDark = false, now = at(23, 30)))
        assertTrue(ThemeMode.AutoTime.isDark(systemDark = false, now = at(0)))
        assertTrue(ThemeMode.AutoTime.isDark(systemDark = false, now = at(6, 59)))
    }

    @Test
    fun `остальные режимы времени не спрашивают`() {
        assertFalse(ThemeMode.Light.isDark(systemDark = true, now = at(23)))
        assertTrue(ThemeMode.Dark.isDark(systemDark = false, now = at(12)))
        assertTrue(ThemeMode.System.isDark(systemDark = true, now = at(12)))
        assertFalse(ThemeMode.System.isDark(systemDark = false, now = at(23)))
    }
}
