package app.plein.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Порядок приложений в поиске.
 *
 * Человек набирает как придётся: две буквы подряд, первые буквы слов или
 * обрывок из середины. Плюс лаунчер помнит, что и когда открывают, — и держит
 * это выше.
 */
class AppRankerTest {

    private fun score(query: String, title: String, launches: Int = 0, atHour: Int = 0, hour: Int = 12) =
        AppRanker.score(
            title = title,
            query = query,
            launches = launches,
            launchesAtHour = atHour,
            totalLaunches = 100,
        )

    @Test
    fun `начало названия сильнее середины`() {
        val head = score("те", "Телеграм")
        val middle = score("гр", "Телеграм")
        assertTrue("начало должно быть выше: $head против $middle", head > middle)
    }

    @Test
    fun `первые буквы слов находят приложение`() {
        assertTrue(score("вк", "ВКонтакте") > 0)
        assertTrue(score("нст", "Настройки") > 0)
        assertTrue(score("гк", "Google Карты") > 0)
    }

    @Test
    fun `чужие буквы не находятся`() {
        assertEquals(0.0, score("xyz", "Телеграм"), 0.001)
        assertEquals(0.0, score("тм", "Google Карты"), 0.001)
    }

    @Test
    fun `частое приложение выше редкого`() {
        val often = score("ка", "Камера", launches = 80)
        val rare = score("ка", "Календарь", launches = 2)
        assertTrue("частое должно быть выше: $often против $rare", often > rare)
    }

    @Test
    fun `привычка этого часа поднимает приложение`() {
        val morning = score("по", "Почта", launches = 10, atHour = 9, hour = 9)
        val plain = score("по", "Погода", launches = 10, atHour = 0, hour = 9)
        assertTrue("утреннее должно быть выше: $morning против $plain", morning > plain)
    }

    @Test
    fun `пустой запрос отдаёт привычку`() {
        val used = score("", "Камера", launches = 50)
        val fresh = score("", "Калькулятор", launches = 0)
        assertTrue(used > fresh)
    }

    @Test
    fun `порядок собирается целиком`() {
        val ranked = AppRanker.rank(
            items = listOf("Календарь" to 1, "Камера" to 60, "Карты" to 5),
            query = "ка",
            hour = 12,
            launchesAt = { _, _ -> 0 },
            total = 66,
        )
        assertEquals("Камера", ranked.first())
    }
}
