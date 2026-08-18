package app.plein.search

import java.net.URLEncoder

/**
 * Магазины приложений: куда уйти, когда на телефоне ничего не нашлось.
 *
 * Список не зашит. Про Play, RuStore и F-Droid лаунчер знает сам, потому что
 * у каждого свой адрес поиска. Остальных он спрашивает у системы: кто подписан
 * на `market://`, тот и магазин — Aurora, Droid-ify, Neo Store или что-то, о
 * чём мы не слышали. Имя такого магазина берётся у него же, поэтому строка
 * читается так, как приложение называет себя само.
 *
 * Показываем только то, что у человека стоит: советовать Play на прошивке без
 * сервисов Google означает отправить его в пустоту. Когда магазина нет вовсе,
 * остаётся F-Droid — его поиск работает и просто в браузере.
 *
 * Адреса проверены 18.08.2026.
 */
object Stores {

    const val PLAY = "play"
    const val RUSTORE = "rustore"
    const val FDROID = "fdroid"
    const val GITHUB = "github"

    /** Магазин в выдаче. Пустой пакет означает «откроется браузером». */
    data class Store(val id: String, val title: String, val packageName: String?)

    /** Кто в системе взялся обрабатывать `market://`. */
    data class Handler(val packageName: String, val label: String)

    private val PACKAGES = mapOf(
        PLAY to setOf("com.android.vending"),
        RUSTORE to setOf("ru.vk.store"),
        FDROID to setOf("org.fdroid.fdroid", "org.fdroid.basic"),
        // Obtainium ставит прямо из репозиториев, своего поиска у него нет:
        // ищем за него на GitHub, откуда он и берёт сборки.
        GITHUB to setOf("dev.imranr.obtainium", "dev.imranr.obtainium.fdroid"),
    )

    private val TITLES = mapOf(
        PLAY to "Google Play",
        RUSTORE to "RuStore",
        FDROID to "F-Droid",
        GITHUB to "GitHub",
    )

    /** Все пакеты, которые лаунчер знает в лицо. */
    val KNOWN: Set<String> = PACKAGES.values.flatten().toSet()

    /** Намерение, которым мы спрашиваем систему про магазины. */
    const val PROBE = "market://search?q=plein"

    /**
     * Что показать в разделе «Установить».
     *
     * Свои идут по краям: Play первым, RuStore и F-Droid за чужими, GitHub
     * последним. Чужие стоят между ними по алфавиту, потому что порядок,
     * в котором их возвращает система, ничего не значит.
     */
    fun list(installed: Set<String>, handlers: List<Handler>): List<Store> {
        val own = mutableListOf<Store>()
        val order = listOf(PLAY, RUSTORE, FDROID, GITHUB)
        order.forEach { id ->
            val pkg = PACKAGES.getValue(id).firstOrNull { it in installed } ?: return@forEach
            own += Store(id, TITLES.getValue(id), pkg)
        }

        val others = handlers
            .filterNot { it.packageName in KNOWN }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .map { Store(it.packageName, it.label, it.packageName) }

        if (own.isEmpty() && others.isEmpty()) {
            return listOf(Store(FDROID, TITLES.getValue(FDROID), null))
        }

        val play = own.filter { it.id == PLAY }
        val rest = own.filterNot { it.id == PLAY }
        return play + others + rest
    }

    /**
     * Ссылка поиска.
     *
     * У Play и у чужих магазинов схема `market://`: её понимают все, кто на
     * неё подписан. У RuStore и F-Droid свой адрес на сайте, у GitHub поиск
     * по репозиториям — оттуда Obtainium и ставит.
     */
    fun url(store: Store, query: String, language: String): String {
        val encoded = encode(query)
        return when (store.id) {
            RUSTORE -> "https://www.rustore.ru/catalog/search?query=$encoded"
            FDROID -> "https://search.f-droid.org/?q=$encoded&lang=$language"
            GITHUB -> "https://github.com/search?q=$encoded&type=repositories"
            else -> "market://search?q=$encoded&c=apps"
        }
    }

    /**
     * Кому адресовать намерение.
     *
     * Схему `market://` ловит не только Play: F-Droid и любой другой клиент
     * тоже на неё подписаны, и без адреса система показывает выбор «чем
     * открыть» вместо магазина. А вот сайт открывает браузер, и пакет там
     * только мешает: клиент F-Droid своих веб-ссылок не ловит.
     */
    fun target(store: Store): String? = when (store.id) {
        FDROID, GITHUB -> null
        else -> store.packageName
    }

    /** Запасной ход, когда схему `market://` подхватить некому. */
    fun webUrl(store: Store, query: String, language: String): String = when (store.id) {
        RUSTORE, FDROID, GITHUB -> url(store, query, language)
        else -> "https://play.google.com/store/search?q=${encode(query)}&c=apps"
    }

    /** Пробел в адресе ломает разбор, а плюс RuStore понимает как плюс. */
    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}
