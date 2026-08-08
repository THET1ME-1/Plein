package app.plein.data

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

/**
 * Кто и когда открывает приложения.
 *
 * Считаем два числа: сколько раз открыли вообще и сколько раз в этот час.
 * Этого хватает, чтобы утром поднять почту, а вечером плеер, и не хватает,
 * чтобы восстановить чей-то распорядок дня — наружу ничего не уходит.
 *
 * Часы храним корзинами по четыре: раскладывать по двадцати четырём означало
 * бы ждать месяц, пока наберётся статистика.
 */
class LaunchStats(context: Context) {

    private val sp = context.getSharedPreferences("plein_usage", Context.MODE_PRIVATE)
    private var counts = load(KEY_TOTAL)
    private var byBucket = load(KEY_HOURS)

    fun remember(key: String) {
        counts[key] = (counts[key] ?: 0) + 1
        val slot = "$key#${bucketOf(nowHour())}"
        byBucket[slot] = (byBucket[slot] ?: 0) + 1
        save()
    }

    fun launches(key: String): Int = counts[key] ?: 0

    fun launchesAt(key: String, hour: Int): Int = byBucket["$key#${bucketOf(hour)}"] ?: 0

    fun total(): Int = counts.values.sum()

    fun nowHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    /** Четыре часа в корзине: утро, день, вечер, ночь и промежутки между ними. */
    private fun bucketOf(hour: Int): Int = (hour / 4).coerceIn(0, 5)

    private fun load(key: String): MutableMap<String, Int> = runCatching {
        val raw = sp.getString(key, null) ?: return mutableMapOf()
        val json = JSONObject(raw)
        val map = mutableMapOf<String, Int>()
        json.keys().forEach { name -> map[name] = json.optInt(name) }
        map
    }.getOrDefault(mutableMapOf())

    private fun save() {
        sp.edit()
            .putString(KEY_TOTAL, JSONObject(counts as Map<*, *>).toString())
            .putString(KEY_HOURS, JSONObject(byBucket as Map<*, *>).toString())
            .apply()
    }

    private companion object {
        const val KEY_TOTAL = "counts"
        const val KEY_HOURS = "hours"
    }
}
