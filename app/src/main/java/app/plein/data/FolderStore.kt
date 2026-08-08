package app.plein.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.res.stringResource
import app.plein.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Папка домашнего экрана в том виде, в каком её правит человек.
 *
 * [appKeys] хранит и состав, и порядок. Папка «Все приложения» состава не имеет:
 * в неё попадает всё установленное, а appKeys задаёт только порядок.
 *
 * [titleKey] стоит у папок, которые лаунчер разложил сам на первом запуске. Пока
 * он есть, заголовок берётся из ресурсов и переезжает вместе со сменой языка.
 * Человек переименовал папку — ключ снимается, и остаётся его текст.
 */
data class FolderConfig(
    val id: String,
    val title: String,
    val appKeys: List<String>,
    val isAll: Boolean = false,
    val titleKey: String? = null,
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
class FolderStore(private val context: Context) {

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
        val seeded = mutableListOf(
            FolderConfig(ALL_ID, context.getString(R.string.all_apps), emptyList(), isAll = true)
        )
        CATEGORIES.forEach { (category, key) ->
            val inCategory = apps.filter { it.category == category }
            if (inCategory.size >= MIN_APPS) {
                seeded += FolderConfig(
                    id = newId(),
                    title = context.getString(FolderTitles.resOf(key)),
                    appKeys = inCategory.map { it.key },
                    titleKey = key,
                )
            }
        }
        folders.addAll(seeded)
        persist()
    }

    fun create(title: String) {
        val name = title.ifBlank { context.getString(R.string.new_folder) }
        folders.add(FolderConfig(newId(), name, emptyList()))
        persist()
    }

    fun rename(id: String, title: String) {
        val index = folders.indexOfFirst { it.id == id }
        if (index < 0 || title.isBlank()) return
        folders[index] = folders[index].copy(title = title, titleKey = null)
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
                    folder.titleKey?.let { put("titleKey", it) }
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
                titleKey = item.optString("titleKey").ifEmpty { null },
            )
        }
    }.getOrDefault(emptyList())

    private fun newId() = UUID.randomUUID().toString().take(8)

    companion object {
        const val ALL_ID = "all"
        private const val KEY_FOLDERS = "folders"
        private const val MIN_APPS = 4

        private val CATEGORIES = listOf(
            ApplicationInfo.CATEGORY_SOCIAL to FolderTitles.SOCIAL,
            ApplicationInfo.CATEGORY_AUDIO to FolderTitles.MUSIC,
            ApplicationInfo.CATEGORY_VIDEO to FolderTitles.VIDEO,
            ApplicationInfo.CATEGORY_IMAGE to FolderTitles.PHOTO,
            ApplicationInfo.CATEGORY_PRODUCTIVITY to FolderTitles.WORK,
            ApplicationInfo.CATEGORY_GAME to FolderTitles.GAMES,
            ApplicationInfo.CATEGORY_MAPS to FolderTitles.MAPS,
        )
    }
}

/** Заголовки папок, которые лаунчер разложил сам. Ключ хранится, текст берётся из ресурсов. */
object FolderTitles {

    const val SOCIAL = "social"
    const val MUSIC = "music"
    const val VIDEO = "video"
    const val PHOTO = "photo"
    const val WORK = "work"
    const val GAMES = "games"
    const val MAPS = "maps"

    @StringRes
    fun resOf(key: String): Int = when (key) {
        SOCIAL -> R.string.folder_social
        MUSIC -> R.string.folder_music
        VIDEO -> R.string.folder_video
        PHOTO -> R.string.folder_photo
        WORK -> R.string.folder_work
        GAMES -> R.string.folder_games
        MAPS -> R.string.folder_maps
        else -> R.string.folders
    }
}

/** Заголовок папки на экране: у своих папок свой текст, у разложенных — перевод. */
@Composable
fun FolderConfig.displayTitle(): String = when {
    isAll -> stringResource(R.string.all_apps)
    titleKey != null -> stringResource(FolderTitles.resOf(titleKey))
    else -> title
}
