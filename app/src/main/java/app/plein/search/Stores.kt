package app.plein.search

import java.net.URLEncoder

/**
 * Магазины приложений: куда уйти, когда на телефоне ничего не нашлось.
 *
 * Показываем только те, что у человека стоят. Советовать Play на прошивке без
 * сервисов Google означает отправить его в пустоту, а RuStore за пределами
 * России не поставлен ни у кого. Когда магазина нет вовсе, остаётся F-Droid:
 * его поиск работает и просто в браузере.
 *
 * Ссылки проверены 18.08.2026, поиск на сайте у всех троих отвечает.
 */
object Stores {

    const val PLAY = "play"
    const val RUSTORE = "rustore"
    const val FDROID = "fdroid"

    /** Порядок показа. Первым идёт то, чем пользуются чаще. */
    private val ORDER = listOf(PLAY, RUSTORE, FDROID)

    private val PACKAGES = mapOf(
        PLAY to setOf("com.android.vending"),
        RUSTORE to setOf("ru.vk.store"),
        FDROID to setOf("org.fdroid.fdroid", "org.fdroid.basic"),
    )

    /** Все пакеты магазинов, какие мы знаем: по ним лаунчер и проверяет. */
    val KNOWN: Set<String> = PACKAGES.values.flatten().toSet()

    fun available(installed: Set<String>): List<String> {
        val found = ORDER.filter { store -> PACKAGES.getValue(store).any { it in installed } }
        return found.ifEmpty { listOf(FDROID) }
    }

    /**
     * Ссылка поиска. У Play своя схема `market://`, её ловит само приложение;
     * у остальных обычный адрес, который открывает либо клиент магазина, либо
     * браузер.
     */
    fun url(store: String, query: String, language: String): String {
        val encoded = encode(query)
        return when (store) {
            PLAY -> "market://search?q=$encoded&c=apps"
            RUSTORE -> "https://www.rustore.ru/catalog/search?query=$encoded"
            else -> "https://search.f-droid.org/?q=$encoded&lang=$language"
        }
    }

    /** Запасной ход, когда схему `market://` подхватить некому. */
    fun webUrl(store: String, query: String, language: String): String = when (store) {
        PLAY -> "https://play.google.com/store/search?q=${encode(query)}&c=apps"
        else -> url(store, query, language)
    }

    /** Пробел в адресе ломает разбор, а плюс RuStore понимает как плюс. */
    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}
