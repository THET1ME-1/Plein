package app.plein.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Своя тема кадра.
 *
 * Человек пишет в настройках, что хочет видеть: «sea» или «snow, winter forest».
 * Слова уходят в фотобанк как есть, а погода и время суток добавляются к ним
 * коротким словом — «sea» плюс дождь даёт «sea rain». Если по такой паре
 * фотобанк молчит, ищем по одной теме: лучше кадр не совсем про сегодняшнюю
 * погоду, чем вшитый.
 */
class BackdropThemeQueriesTest {

    @Test
    fun `тема разбирается по запятым`() {
        assertEquals(listOf("sea"), BackdropQueries.themeWords("sea"))
        assertEquals(listOf("sea", "snow"), BackdropQueries.themeWords("sea, snow"))
        assertEquals(
            listOf("winter forest", "snow city"),
            BackdropQueries.themeWords("  winter forest ,  snow city  "),
        )
    }

    @Test
    fun `пустая тема означает прежнее поведение`() {
        assertTrue(BackdropQueries.themeWords("").isEmpty())
        assertTrue(BackdropQueries.themeWords("   ").isEmpty())
        assertTrue(BackdropQueries.themeWords(" , , ").isEmpty())
    }

    @Test
    fun `перевод строки и точка с запятой тоже разделяют`() {
        assertEquals(listOf("sea", "snow"), BackdropQueries.themeWords("sea; snow"))
        assertEquals(listOf("sea", "snow"), BackdropQueries.themeWords("sea\nsnow"))
    }

    @Test
    fun `погода приклеивается к теме`() {
        assertEquals(
            listOf("sea rain", "snow rain"),
            BackdropQueries.combine(listOf("sea", "snow"), "rain"),
        )
    }

    @Test
    fun `без подсказки тема идёт как есть`() {
        val theme = listOf("sea", "snow")
        assertEquals(theme, BackdropQueries.combine(theme, null))
        assertEquals(theme, BackdropQueries.combine(theme, "  "))
    }

    @Test
    fun `у погоды есть короткое слово`() {
        assertEquals("rain", BackdropQueries.weatherWord(61))
        assertEquals("snow", BackdropQueries.weatherWord(71))
        assertEquals("fog", BackdropQueries.weatherWord(45))
        assertEquals("clear sky", BackdropQueries.weatherWord(0))
        assertEquals("clouds", BackdropQueries.weatherWord(3))
        assertEquals(null, BackdropQueries.weatherWord(-1))
    }

    @Test
    fun `у времени суток есть короткое слово`() {
        assertEquals("sunrise", BackdropQueries.timeWord(DayPart.Morning))
        assertEquals("sunset", BackdropQueries.timeWord(DayPart.Evening))
        assertEquals("night", BackdropQueries.timeWord(DayPart.Night))
        // Днём подсказка только испортила бы запрос: «sea day» — это не про свет.
        assertEquals(null, BackdropQueries.timeWord(DayPart.Day))
    }

    @Test
    fun `слово погоды сходится с прежними запросами`() {
        // Коды, по которым раньше подбирались фразы, теперь дают и слово.
        listOf(0, 1, 2, 3, 45, 48, 61, 71, 80, 95).forEach { code ->
            assertTrue("код $code остался без слова", BackdropQueries.weatherWord(code) != null)
            assertTrue("код $code остался без фраз", BackdropQueries.forWeather(code) != null)
        }
    }
}
