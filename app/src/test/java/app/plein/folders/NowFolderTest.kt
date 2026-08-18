package app.plein.folders

import app.plein.data.NowFolder
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Папка «Сейчас»: то, что человек открывает в этот час.
 *
 * Лаунчер давно считает, в какое время дня что запускают — эта статистика
 * работает в поиске. Отсюда собирается папка, которая утром показывает почту,
 * вечером плеер, а ночью читалку, и настраивать её не нужно вовсе.
 */
class NowFolderTest {

    private fun app(key: String, atHour: Int, total: Int) = NowFolder.Candidate(key, atHour, total)

    @Test
    fun `привычка этого часа важнее общей`() {
        val picked = NowFolder.pick(
            listOf(
                app("почта", atHour = 9, total = 20),
                app("плеер", atHour = 0, total = 300),
            ),
            limit = 5,
        )
        assertEquals(listOf("почта", "плеер"), picked)
    }

    @Test
    fun `в этот час никого — берём то, что открывают вообще`() {
        val picked = NowFolder.pick(
            listOf(app("камера", 0, 4), app("браузер", 0, 40)),
            limit = 5,
        )
        assertEquals(listOf("браузер", "камера"), picked)
    }

    @Test
    fun `нетронутое в папку не идёт`() {
        val picked = NowFolder.pick(listOf(app("часы", 0, 0), app("браузер", 0, 3)), limit = 5)
        assertEquals(listOf("браузер"), picked)
    }

    @Test
    fun `лимит режет хвост`() {
        val many = (1..20).map { app("app$it", atHour = it, total = it) }
        assertEquals(6, NowFolder.pick(many, limit = 6).size)
    }

    @Test
    fun `пустая статистика даёт пустую папку`() {
        assertEquals(emptyList<String>(), NowFolder.pick(emptyList(), limit = 6))
    }
}
