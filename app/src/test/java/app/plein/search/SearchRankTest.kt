package app.plein.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ряд под пустой строкой поиска.
 *
 * Считаем не запуски вообще, а открытия именно из поиска: приложение, до
 * которого человек каждый раз добирается набором букв, ему и нужно под рукой.
 * То, что стоит на домашнем экране, в этот ряд не лезет.
 */
class SearchRankTest {

    private fun opened(count: Int, last: Long) = SearchRank.Use(count, last)

    @Test
    fun `пока поиском не пользовались, ряда нет`() {
        assertEquals(emptyList<String>(), SearchRank.top(emptyMap(), limit = 5))
    }

    @Test
    fun `чаще искали — выше стоит`() {
        val use = mapOf(
            "a" to opened(2, 100),
            "b" to opened(9, 100),
            "c" to opened(5, 100),
        )
        assertEquals(listOf("b", "c", "a"), SearchRank.top(use, limit = 5))
    }

    @Test
    fun `при равном счёте свежее стоит выше`() {
        val use = mapOf("a" to opened(3, 100), "b" to opened(3, 900))
        assertEquals(listOf("b", "a"), SearchRank.top(use, limit = 5))
    }

    @Test
    fun `лимит режет хвост`() {
        val use = (1..9).associate { "app$it" to opened(it, 100) }
        assertEquals(3, SearchRank.top(use, limit = 3).size)
    }

    @Test
    fun `открытое один раз в ряд не идёт`() {
        // Один заход это случайность: так в ряд попадал бы всякий калькулятор,
        // открытый однажды из любопытства.
        val use = mapOf("a" to opened(1, 100), "b" to opened(2, 100))
        assertEquals(listOf("b"), SearchRank.top(use, limit = 5))
    }

    @Test
    fun `забытое полгода назад выпадает`() {
        val fresh = 200L * 24 * 3600_000
        val use = mapOf(
            "old" to opened(40, fresh - 200L * 24 * 3600_000),
            "new" to opened(3, fresh),
        )
        val top = SearchRank.top(use, limit = 5, now = fresh)
        assertEquals(listOf("new"), top)
        assertTrue("старое не должно попадать", !top.contains("old"))
    }
}
