package app.plein.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Подпись автора с Викисклада.
 *
 * Поле Artist приходит куском HTML со ссылками и сущностями: на экран должна
 * попасть одна короткая строка, а не разметка.
 */
class WikimediaSourceTest {

    @Test
    fun `ссылка превращается в имя`() {
        val html = """<a href="//commons.wikimedia.org/wiki/User:Ivan" title="User:Ivan">Ivan Petrov</a>"""
        assertEquals("Ivan Petrov", WikimediaSource.plainText(html))
    }

    @Test
    fun `сущности разворачиваются`() {
        assertEquals("Sun & Moon", WikimediaSource.plainText("Sun &amp; Moon"))
        assertEquals("\"Дом\"", WikimediaSource.plainText("&quot;Дом&quot;"))
    }

    @Test
    fun `длинная подпись режется`() {
        val long = "Очень длинное имя автора, которое никуда не влезет на экране телефона"
        assertTrue(WikimediaSource.plainText(long).length <= 40)
    }

    @Test
    fun `пустое остаётся пустым`() {
        assertEquals("", WikimediaSource.plainText(""))
        assertEquals("", WikimediaSource.plainText("<span></span>"))
    }
}
