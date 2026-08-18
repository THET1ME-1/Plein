package app.plein.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    /**
     * Счётчик растёт сразу, на диск уходит в фоне.
     *
     * Вызов идёт из `launch()` прямо перед стартом чужого приложения, то есть
     * на главном потоке в самый неудачный момент: сериализация двух карт в
     * JSON занимала кадр ровно там, где виден отклик на касание.
     */
    fun remember(key: String) {
        synchronized(this) {
            counts[key] = (counts[key] ?: 0) + 1
            val slot = "$key#${bucketOf(nowHour())}"
            byBucket[slot] = (byBucket[slot] ?: 0) + 1
        }
        io.launch { save() }
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

    /** Снимок под замком: карту нельзя сериализовать, пока её правят. */
    private fun save() {
        val (total, hours) = synchronized(this) {
            JSONObject(counts.toMap() as Map<*, *>).toString() to
                JSONObject(byBucket.toMap() as Map<*, *>).toString()
        }
        sp.edit().putString(KEY_TOTAL, total).putString(KEY_HOURS, hours).apply()
    }

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private companion object {
        const val KEY_TOTAL = "counts"
        const val KEY_HOURS = "hours"
    }
}
