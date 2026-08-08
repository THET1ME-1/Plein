package app.plein.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Резервная копия лаунчера.
 *
 * Один файл JSON: настройки, папки со своим порядком и свои названия
 * приложений. Забирается и кладётся обратно через системный выбор файла,
 * поэтому копия живёт где угодно — в облаке, в мессенджере, на карте.
 *
 * Кадр в копию не едет: он лежит в кэше и на другом телефоне всё равно
 * недоступен, а палитра считается из него заново.
 */
object Backup {

    private const val VERSION = 1
    private val STORES = listOf("plein", "plein_folders", "plein_labels")
    private const val SKIP_PREFIX = "backdrop_"

    fun fileName(): String = "plein-backup.json"

    fun export(context: Context, uri: Uri): Boolean = runCatching {
        val root = JSONObject()
            .put("app", "Plein")
            .put("version", VERSION)

        STORES.forEach { store ->
            val sp = context.getSharedPreferences(store, Context.MODE_PRIVATE)
            val values = JSONObject()
            sp.all.forEach { (key, value) ->
                if (key.startsWith(SKIP_PREFIX)) return@forEach
                encode(value)?.let { values.put(key, it) }
            }
            root.put(store, values)
        }

        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(root.toString(2).toByteArray())
        } ?: return false
        true
    }.getOrDefault(false)

    /**
     * Возврат копии.
     *
     * Каждое хранилище переписывается целиком: слияние со старым оставило бы
     * папки, которых в копии нет, и человек получил бы смесь двух телефонов.
     */
    fun import(context: Context, uri: Uri): Boolean = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().readText()
        } ?: return false
        val root = JSONObject(text)
        if (root.optString("app") != "Plein") return false

        STORES.forEach { store ->
            val values = root.optJSONObject(store) ?: return@forEach
            val editor = context.getSharedPreferences(store, Context.MODE_PRIVATE).edit()
            editor.clear()
            values.keys().forEach { key ->
                val item = values.optJSONObject(key) ?: return@forEach
                when (item.optString("t")) {
                    "s" -> editor.putString(key, item.optString("v"))
                    "i" -> editor.putInt(key, item.optInt("v"))
                    "l" -> editor.putLong(key, item.optLong("v"))
                    "f" -> editor.putFloat(key, item.optDouble("v").toFloat())
                    "b" -> editor.putBoolean(key, item.optBoolean("v"))
                }
            }
            editor.commit()
        }
        true
    }.getOrDefault(false)

    /** Тип хранится рядом со значением: JSON не различает целое и дробное. */
    private fun encode(value: Any?): JSONObject? = when (value) {
        is String -> typed("s", value)
        is Boolean -> typed("b", value)
        is Int -> typed("i", value)
        is Long -> typed("l", value)
        is Float -> typed("f", value.toDouble())
        else -> null
    }

    private fun typed(type: String, value: Any) = JSONObject().put("t", type).put("v", value)

    /** Сколько всего уедет в файл — показываем человеку до нажатия. */
    fun countOf(context: Context): Int = STORES.sumOf { store ->
        context.getSharedPreferences(store, Context.MODE_PRIVATE).all
            .count { !it.key.startsWith(SKIP_PREFIX) }
    }

    /** Папок в копии: их считает не число ключей, а разбор списка. */
    fun folderCount(context: Context): Int = runCatching {
        val raw = context.getSharedPreferences("plein_folders", Context.MODE_PRIVATE)
            .getString("folders", null) ?: return 0
        JSONArray(raw).length()
    }.getOrDefault(0)
}
