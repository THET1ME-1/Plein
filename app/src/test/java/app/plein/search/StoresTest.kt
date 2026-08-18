package app.plein.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Куда идти, когда приложение не нашлось на телефоне.
 *
 * Предлагаем только те магазины, которые у человека стоят: советовать Play
 * на прошивке без сервисов Google бессмысленно.
 */
class StoresTest {

    @Test
    fun `предлагаем только установленные магазины`() {
        val found = Stores.available(setOf("com.android.vending"))
        assertEquals(listOf(Stores.PLAY), found)
    }

    @Test
    fun `рустор и эф-дроид тоже узнаются`() {
        val found = Stores.available(setOf("ru.vk.store", "org.fdroid.fdroid"))
        assertEquals(listOf(Stores.RUSTORE, Stores.FDROID), found)
    }

    @Test
    fun `без магазинов остаётся эф-дроид с сайтом`() {
        assertEquals(listOf(Stores.FDROID), Stores.available(emptySet()))
    }

    @Test
    fun `у плея своя схема, чтобы открылось приложение`() {
        assertEquals("market://search?q=telegram&c=apps", Stores.url(Stores.PLAY, "telegram", "ru"))
    }

    @Test
    fun `эф-дроид ищет на своём сайте и знает язык`() {
        assertEquals("https://search.f-droid.org/?q=telegram&lang=ru", Stores.url(Stores.FDROID, "telegram", "ru"))
    }

    @Test
    fun `рустор ищет по каталогу`() {
        assertEquals("https://www.rustore.ru/catalog/search?query=telegram", Stores.url(Stores.RUSTORE, "telegram", "ru"))
    }

    @Test
    fun `пробелы и кириллица уезжают закодированными`() {
        val link = Stores.url(Stores.FDROID, "новая почта", "ru")
        assertTrue("пробел остался сырым: $link", !link.contains(" "))
        assertTrue("кириллица не закодирована: $link", !link.contains("новая"))
        assertTrue("плюс вместо пробела: $link", !link.contains("+"))
    }
}
