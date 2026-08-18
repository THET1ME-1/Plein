package app.plein.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Куда идти, когда приложение не нашлось на телефоне.
 *
 * Список магазинов не зашит: у кого что стоит, тот то и видит. Про Play,
 * RuStore и F-Droid мы знаем сами, остальных спрашиваем у системы — кто
 * подписан на `market://`, тот и магазин, будь это Aurora, Droid-ify или
 * что-то, о чём мы не слышали.
 */
class StoresTest {

    private fun handler(pkg: String, label: String) = Stores.Handler(pkg, label)

    @Test
    fun `предлагаем только установленные магазины`() {
        val found = Stores.list(installed = setOf("com.android.vending"), handlers = emptyList())
        assertEquals(listOf("Google Play"), found.map { it.title })
    }

    @Test
    fun `рустор и эф-дроид узнаются по пакету`() {
        val found = Stores.list(setOf("ru.vk.store", "org.fdroid.fdroid"), emptyList())
        assertEquals(listOf("RuStore", "F-Droid"), found.map { it.title })
    }

    @Test
    fun `чужой магазин приходит из системы вместе со своим именем`() {
        val found = Stores.list(emptySet(), listOf(handler("com.aurora.store", "Aurora Store")))
        assertEquals(listOf("Aurora Store"), found.map { it.title })
        assertEquals("com.aurora.store", found.first().packageName)
    }

    @Test
    fun `знакомый магазин не задваивается`() {
        // F-Droid тоже подписан на market://, и без склейки он шёл бы дважды.
        val found = Stores.list(
            installed = setOf("com.android.vending", "org.fdroid.fdroid"),
            handlers = listOf(handler("com.android.vending", "Google Play Маркет"), handler("org.fdroid.fdroid", "F-Droid")),
        )
        assertEquals(listOf("Google Play", "F-Droid"), found.map { it.title })
    }

    @Test
    fun `свои идут первыми, чужие между ними по алфавиту`() {
        val found = Stores.list(
            installed = setOf("com.android.vending", "org.fdroid.fdroid"),
            handlers = listOf(handler("com.looker.droidify", "Droid-ify"), handler("com.aurora.store", "Aurora Store")),
        )
        assertEquals(listOf("Google Play", "Aurora Store", "Droid-ify", "F-Droid"), found.map { it.title })
    }

    @Test
    fun `обтениум ведёт на поиск по гитхабу`() {
        val found = Stores.list(setOf("dev.imranr.obtainium"), emptyList())
        assertEquals(listOf("GitHub"), found.map { it.title })
        assertEquals(
            "https://github.com/search?q=telegram&type=repositories",
            Stores.url(found.first(), "telegram", "ru"),
        )
    }

    @Test
    fun `без магазинов остаётся эф-дроид с сайтом`() {
        val found = Stores.list(emptySet(), emptyList())
        assertEquals(listOf("F-Droid"), found.map { it.title })
        assertEquals(null, found.first().packageName)
    }

    @Test
    fun `у плея своя схема, чтобы открылось приложение`() {
        val play = Stores.list(setOf("com.android.vending"), emptyList()).first()
        assertEquals("market://search?q=telegram&c=apps", Stores.url(play, "telegram", "ru"))
        assertEquals("com.android.vending", play.packageName)
    }

    @Test
    fun `чужой магазин открывается той же схемой, но адресно`() {
        val aurora = Stores.list(emptySet(), listOf(handler("com.aurora.store", "Aurora Store"))).first()
        assertEquals("market://search?q=telegram&c=apps", Stores.url(aurora, "telegram", "ru"))
    }

    @Test
    fun `эф-дроид ищет на своём сайте и знает язык`() {
        val fdroid = Stores.list(emptySet(), emptyList()).first()
        assertEquals("https://search.f-droid.org/?q=telegram&lang=ru", Stores.url(fdroid, "telegram", "ru"))
    }

    @Test
    fun `рустор ищет по каталогу`() {
        val rustore = Stores.list(setOf("ru.vk.store"), emptyList()).first()
        assertEquals("https://www.rustore.ru/catalog/search?query=telegram", Stores.url(rustore, "telegram", "ru"))
    }

    @Test
    fun `у плея есть запасной ход в браузер`() {
        val play = Stores.list(setOf("com.android.vending"), emptyList()).first()
        assertEquals(
            "https://play.google.com/store/search?q=telegram&c=apps",
            Stores.webUrl(play, "telegram", "ru"),
        )
    }

    @Test
    fun `пробелы и кириллица уезжают закодированными`() {
        val fdroid = Stores.list(emptySet(), emptyList()).first()
        val link = Stores.url(fdroid, "новая почта", "ru")
        assertTrue("пробел остался сырым: $link", !link.contains(" "))
        assertTrue("кириллица не закодирована: $link", !link.contains("новая"))
        assertTrue("плюс вместо пробела: $link", !link.contains("+"))
    }

    @Test
    fun `сайт открывается браузером, а не пакетом магазина`() {
        val fdroid = Stores.list(setOf("org.fdroid.fdroid"), emptyList()).first()
        val github = Stores.list(setOf("dev.imranr.obtainium"), emptyList()).first()
        assertEquals(null, Stores.target(fdroid))
        assertEquals(null, Stores.target(github))
    }

    @Test
    fun `магазин со своей схемой получает адрес`() {
        val play = Stores.list(setOf("com.android.vending"), emptyList()).first()
        val aurora = Stores.list(emptySet(), listOf(handler("com.aurora.store", "Aurora Store"))).first()
        val rustore = Stores.list(setOf("ru.vk.store"), emptyList()).first()
        assertEquals("com.android.vending", Stores.target(play))
        assertEquals("com.aurora.store", Stores.target(aurora))
        assertEquals("ru.vk.store", Stores.target(rustore))
    }
}
