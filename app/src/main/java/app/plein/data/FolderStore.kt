package app.plein.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Папка домашнего экрана в том виде, в каком её правит человек.
 *
 * [appKeys] хранит и состав, и порядок. Папка «Все приложения» состава не имеет:
 * в неё попадает всё установленное, а appKeys задаёт только порядок.
 */
data class FolderConfig(
    val id: String,
    val title: String,
    val appKeys: List<String>,
    val isAll: Boolean = false,
) {
    fun resolve(all: List<AppEntry>): Folder {
        val byKey = all.associateBy { it.key }
        val ordered = appKeys.mapNotNull { byKey[it] }
        val rest = if (isAll) all.filterNot { it.key in appKeys } else emptyList()
        return Folder(title = title, apps = ordered + rest)
    }
}

/**
 * Хранилище папок.
 *
 * Лежит в SharedPreferences строкой JSON: домашний экран читает его до первого
 * кадра, и база данных здесь означала бы пустой экран на старте.
 */
class FolderStore(context: Context) {

    private val sp = context.getSharedPreferences("plein_folders", Context.MODE_PRIVATE)

    val folders = mutableStateListOf<FolderConfig>()

    init {
        val stored = sp.getString(KEY_FOLDERS, null)
        if (stored != null) {
            folders.addAll(parse(stored))
        }
    }

    /** Первый запуск: раскладываем по категориям, дальше человек правит руками. */
    fun seedIfEmpty(apps: List<AppEntry>) {
        if (folders.isNotEmpty() || apps.isEmpty()) return
        val seeded = mutableListOf(FolderConfig(ALL_ID, "Все приложения", emptyList(), isAll = true))
        CATEGORIES.forEach { (category, title) ->
            val inCategory = apps.filter { it.category == category }
            if (inCategory.size >= MIN_APPS) {
                seeded += FolderConfig(newId(), title, inCategory.map { it.key })
            }
        }
        folders.addAll(seeded)
        persist()
    }

    fun create(title: String) {
        folders.add(FolderConfig(newId(), title.ifBlank { "Новая папка" }, emptyList()))
        persist()
    }

    fun rename(id: String, title: String) {
        val index = folders.indexOfFirst { it.id == id }
        if (index < 0 || title.isBlank()) return
        folders[index] = folders[index].copy(title = title)
        persist()
    }

    fun delete(id: String) {
        if (id == ALL_ID) return
        folders.removeAll { it.id == id }
        persist()
    }

    fun moveFolder(from: Int, to: Int) {
        if (from !in folders.indices || to !in folders.indices) return
        val item = folders.removeAt(from)
        folders.add(to, item)
        persist()
    }

    /** Порядок внутри папки: ключи в том виде, в каком человек их перетащил. */
    fun setOrder(id: String, keys: List<String>) {
        val index = folders.indexOfFirst { it.id == id }
        if (index < 0) return
        folders[index] = folders[index].copy(appKeys = keys)
        persist()
    }

    fun toggleMembership(folderId: String, appKey: String) {
        val index = folders.indexOfFirst { it.id == folderId }
        if (index < 0) return
        val folder = folders[index]
        if (folder.isAll) return
        val keys = folder.appKeys.toMutableList()
        if (!keys.remove(appKey)) keys.add(appKey)
        folders[index] = folder.copy(appKeys = keys)
        persist()
    }

    fun foldersWith(appKey: String): Set<String> =
        folders.filter { !it.isAll && appKey in it.appKeys }.map { it.id }.toSet()

    private fun persist() {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject().apply {
                    put("id", folder.id)
                    put("title", folder.title)
                    put("isAll", folder.isAll)
                    put("apps", JSONArray(folder.appKeys))
                }
            )
        }
        sp.edit().putString(KEY_FOLDERS, array.toString()).apply()
    }

    private fun parse(raw: String): List<FolderConfig> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            val keys = item.optJSONArray("apps") ?: JSONArray()
            FolderConfig(
                id = item.getString("id"),
                title = item.getString("title"),
                appKeys = (0 until keys.length()).map { keys.getString(it) },
                isAll = item.optBoolean("isAll", false),
            )
        }
    }.getOrDefault(emptyList())

    private fun newId() = UUID.randomUUID().toString().take(8)

    companion object {
        const val ALL_ID = "all"
        private const val KEY_FOLDERS = "folders"
        private const val MIN_APPS = 4

        private val CATEGORIES = listOf(
            ApplicationInfo.CATEGORY_SOCIAL to "Общение",
            ApplicationInfo.CATEGORY_AUDIO to "Музыка",
            ApplicationInfo.CATEGORY_VIDEO to "Видео",
            ApplicationInfo.CATEGORY_IMAGE to "Фото",
            ApplicationInfo.CATEGORY_PRODUCTIVITY to "Работа",
            ApplicationInfo.CATEGORY_GAME to "Игры",
            ApplicationInfo.CATEGORY_MAPS to "Карты",
        )
    }
}
