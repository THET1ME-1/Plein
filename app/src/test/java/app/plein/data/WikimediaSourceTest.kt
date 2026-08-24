package app.plein.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

/**
 * Пропорции кадра.
 *
 * Шапка лаунчера — лежачая полоса, и прошлая проверка пропускала ровно
 * наоборот: только стоячие снимки. В подборках их три из сорока пяти, поэтому
 * категория кончалась с третьего кадра и лаунчер отвечал «ничего не отдал».
 */
class WikimediaFitTest {

    @Test
    fun `обычный горизонтальный годится`() {
        assertTrue(WikimediaSource.fitsScreen(1440, 960))
        assertTrue(WikimediaSource.fitsScreen(1920, 1080))
    }

    @Test
    fun `стоячий тоже годится`() {
        assertTrue(WikimediaSource.fitsScreen(1080, 1440))
    }

    @Test
    fun `панорама и башня отсекаются`() {
        assertFalse("панорама 9:1", WikimediaSource.fitsScreen(4500, 500))
        assertFalse("башня 1:3", WikimediaSource.fitsScreen(600, 1800))
    }

    @Test
    fun `пустые размеры не проходят`() {
        assertFalse(WikimediaSource.fitsScreen(0, 0))
    }
}

/**
 * Адрес запроса к Викискладу.
 *
 * Категории и поиск словами — две разные ветки API. Категорий на «sea» или
 * «snow» там нет, поэтому своя тема ищется полнотекстово, по файлам-картинкам.
 */
class WikimediaUrlTest {

    @Test
    fun `тема ищется словами среди файлов`() {
        val url = WikimediaSource.searchUrl("sea rain", width = 1512, offset = 0)
        assertTrue(url, url.contains("generator=search"))
        assertTrue(url, url.contains("gsrnamespace=6"))
        assertTrue("тема потерялась: $url", url.contains("sea+rain") || url.contains("sea%20rain"))
        assertTrue("нужны растровые картинки: $url", url.contains("filetype%3Abitmap"))
        assertTrue(url, url.contains("iiurlwidth=1512"))
    }

    @Test
    fun `пробелы и кириллица кодируются`() {
        val url = WikimediaSource.searchUrl("зимний лес", width = 1080, offset = 50)
        assertFalse("пробел ушёл в адрес сырым: $url", url.contains(" "))
        assertTrue(url, url.contains("gsroffset=50"))
    }

    @Test
    fun `категория ищется по своей ветке`() {
        val url = WikimediaSource.categoryUrl("Category:Quality images of sunsets", width = 1080, from = "a")
        assertTrue(url, url.contains("generator=categorymembers"))
        assertTrue(url, url.contains("gcmtype=file"))
        assertFalse("категория не должна ходить поиском: $url", url.contains("generator=search"))
    }
}
