package app.plein.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Круговая папка: приложения стоят кольцом внутри клетки 2×2.
 *
 * От страницы-подборки отличается тем, что живёт прямо на сетке рядом с
 * плитками и держит свой состав. Шесть первых видны кольцом и запускаются
 * одним касанием, остальные лежат за нажатием в середину.
 */
data class RingFolder(
    val id: String,
    val title: String,
    val appKeys: List<String>,
) {
    /** Что видно кольцом. */
    fun ring(): List<String> = appKeys.take(RingFolders.RING)

    /** Сколько осталось за серединой. */
    fun rest(): Int = (appKeys.size - RingFolders.RING).coerceAtLeast(0)
}

class RingFolders(context: Context) {

    private val sp = context.getSharedPreferences("plein_rings", Context.MODE_PRIVATE)
    private val cache = mutableStateMapOf<String, RingFolder>()

    init {
        sp.all.keys.forEach { id -> parse(id, sp.getString(id, null))?.let { cache[id] = it } }
    }

    fun get(id: String): RingFolder? = cache[id]

    /** Завести папку и вернуть её номер: раскладка кладёт его в свою клетку. */
    fun create(title: String): String {
        val id = UUID.randomUUID().toString().take(8)
        save(RingFolder(id = id, title = title, appKeys = emptyList()))
        return id
    }

    fun rename(id: String, title: String) {
        cache[id]?.let { save(it.copy(title = title)) }
    }

    fun setApps(id: String, keys: List<String>) {
        cache[id]?.let { save(it.copy(appKeys = keys)) }
    }

    fun remove(id: String) {
        cache.remove(id)
        sp.edit().remove(id).apply()
    }

    private fun save(folder: RingFolder) {
        cache[folder.id] = folder
        val body = JSONObject().apply {
            put("title", folder.title)
            put("apps", JSONArray().apply { folder.appKeys.forEach { put(it) } })
        }
        sp.edit().putString(folder.id, body.toString()).apply()
    }

    private fun parse(id: String, raw: String?): RingFolder? = runCatching {
        val body = JSONObject(raw ?: return null)
        val apps = body.optJSONArray("apps") ?: JSONArray()
        RingFolder(
            id = id,
            title = body.optString("title"),
            appKeys = (0 until apps.length()).map { apps.getString(it) },
        )
    }.getOrNull()

    companion object {

        /** Сколько значков стоит кольцом. Дальше — за серединой. */
        const val RING = 6

        /** Радиус кольца долей от стороны папки. */
        const val RADIUS = 0.32f

        /** Значок в кольце долей от стороны папки. */
        const val ICON = 0.26f

        /** Середина: область нажатия под раскрытие. */
        const val CORE = 0.28f

        /**
         * Где стоит значок с номером [index] при [count] местах.
         *
         * Первое место сверху, дальше по часовой стрелке. Возвращает смещение
         * от середины папки долей её стороны.
         */
        fun offsetOf(index: Int, count: Int): Pair<Float, Float> {
            if (count <= 0) return 0f to 0f
            val angle = -PI / 2 + 2 * PI * index / count
            return (RADIUS * cos(angle)).toFloat() to (RADIUS * sin(angle)).toFloat()
        }

    }
}
