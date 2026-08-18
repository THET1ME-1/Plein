package app.plein.data

import android.content.Context
import app.plein.search.SearchRank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Что человек открывает через поиск.
 *
 * Отдельно от `LaunchStats`: там все запуски подряд, включая значки на
 * домашнем экране, и ряд под строкой поиска от них наполняется тем, что и так
 * лежит перед глазами. Здесь только то, до чего добираются набором букв.
 *
 * Наружу ничего не уходит: два числа на приложение, счёт и время последнего
 * открытия.
 */
class SearchStats(context: Context) {

    private val sp = context.getSharedPreferences("plein_search", Context.MODE_PRIVATE)
    private val use = load()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Счётчик растёт сразу, на диск уходит в фоне: касание значка ждать не должно. */
    fun remember(key: String, now: Long = System.currentTimeMillis()) {
        synchronized(this) {
            val was = use[key]
            use[key] = SearchRank.Use((was?.count ?: 0) + 1, now)
        }
        io.launch { save() }
    }

    fun top(limit: Int, now: Long = System.currentTimeMillis()): List<String> =
        SearchRank.top(snapshot(), limit, now)

    private fun snapshot(): Map<String, SearchRank.Use> = synchronized(this) { use.toMap() }

    private fun load(): MutableMap<String, SearchRank.Use> = runCatching {
        val raw = sp.getString(KEY, null) ?: return mutableMapOf()
        val json = JSONObject(raw)
        val map = mutableMapOf<String, SearchRank.Use>()
        json.keys().forEach { key ->
            val parts = json.optString(key).split(':')
            val count = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
            val last = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            map[key] = SearchRank.Use(count, last)
        }
        map
    }.getOrDefault(mutableMapOf())

    private fun save() {
        val json = JSONObject()
        snapshot().forEach { (key, value) -> json.put(key, "${value.count}:${value.lastAt}") }
        sp.edit().putString(KEY, json.toString()).apply()
    }

    private companion object {
        const val KEY = "opened"
    }
}
