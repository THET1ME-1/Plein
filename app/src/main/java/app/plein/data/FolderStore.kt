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
    /** Ключ категории, если папку ведёт правило. Пусто — папка ручная. */
    val rule: String? = null,
    /** Что человек вынул из живой папки руками: правило это уважает. */
    val removed: List<String> = emptyList(),
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

    /**
     * Первый запуск: заводим только «Все приложения».
     *
     * Раньше лаунчер сам раскладывал папки по категориям, и человек получал
     * чужую структуру, которую надо разбирать. Теперь папки по категориям
     * включаются поштучно в настройках.
     */
    fun seedIfEmpty(apps: List<AppEntry>) {
        if (folders.isNotEmpty() || apps.isEmpty()) return
        folders.add(FolderConfig(ALL_ID, context.getString(R.string.all_apps), emptyList(), isAll = true))
        persist()
    }

    /** Сколько приложений попадёт в папку категории: показываем в настройках. */
    fun countFor(key: String, apps: List<AppEntry>, stats: LaunchStats? = null): Int =
        matchingFor(key, apps, stats).size

    fun ruled(key: String): FolderConfig? = folders.firstOrNull { it.rule == key }

    /**
     * Включить или выключить папку категории.
     *
     * Выключение стирает папку целиком: она собиралась правилом, своего в ней
     * ничего нет. Приложения при этом никуда не деваются, они и так все лежат
     * в «Всех приложениях».
     */
    fun setCategoryFolder(key: String, enabled: Boolean, apps: List<AppEntry>, stats: LaunchStats? = null) {
        val existing = ruled(key)
        if (!enabled) {
            existing?.let { folders.remove(it) }
            persist()
            return
        }
        if (existing != null) return
        folders.add(
            FolderConfig(
                id = newId(),
                title = context.getString(FolderTitles.resOf(key)),
                appKeys = matchingFor(key, apps, stats),
                titleKey = key,
                rule = key,
            )
        )
        persist()
    }

    /**
     * Пересобрать живые папки под нынешний набор приложений.
     *
     * Зовётся при каждом изменении списка: поставили приложение — оно легло
     * в свою папку, удалили — ушло. Ручные папки не трогаем вовсе.
     */
    fun syncRules(apps: List<AppEntry>, stats: LaunchStats? = null) {
        if (apps.isEmpty()) return
        var changed = false
        folders.forEachIndexed { index, folder ->
            val key = folder.rule ?: return@forEachIndexed
            val matching = matchingFor(key, apps, stats)
            // «Сейчас» описывает привычку целиком, поэтому состав у неё
            // задаёт правило, а не прежний набор: час сменился — сменилась и
            // папка. У категорий наоборот, там ручная расстановка важнее.
            val next = if (key == FolderTitles.NOW) {
                matching.filterNot { it in folder.removed }
            } else {
                FolderRules.apply(
                    current = folder.appKeys,
                    matching = matching,
                    removed = folder.removed.toSet(),
                    keepStrangers = true,
                )
            }
            if (next != folder.appKeys) {
                folders[index] = folder.copy(appKeys = next)
                changed = true
            }
        }
        if (changed) persist()
    }

    private fun categoryOf(key: String): Int? = CATEGORIES.firstOrNull { it.second == key }?.first

    /** Кто попадает в папку по её правилу. */
    private fun matchingFor(key: String, apps: List<AppEntry>, stats: LaunchStats?): List<String> {
        if (key == FolderTitles.NOW) {
            val counter = stats ?: return emptyList()
            val hour = counter.nowHour()
            val keys = apps.map { it.key }.toSet()
            return NowFolder.pick(
                apps.map { entry ->
                    NowFolder.Candidate(
                        key = entry.key,
                        atHour = counter.launchesAt(entry.key, hour),
                        total = counter.launches(entry.key),
                    )
                }
            ).filter { it in keys }
        }
        val category = categoryOf(key) ?: return emptyList()
        return apps.filter { it.category == category }.map { it.key }
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
        val taken = keys.remove(appKey)
        if (!taken) keys.add(appKey)
        // У живой папки помним, что вынесли руками: иначе правило вернёт
        // приложение обратно на следующем же пересчёте.
        val removed = folder.removed.toMutableList()
        if (folder.rule != null) {
            if (taken) removed += appKey else removed -= appKey
        }
        folders[index] = folder.copy(appKeys = keys, removed = removed.distinct())
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
                    folder.rule?.let { put("rule", it) }
                    if (folder.removed.isNotEmpty()) put("removed", JSONArray(folder.removed))
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
                rule = item.optString("rule").ifEmpty { null },
                removed = item.optJSONArray("removed")?.let { list ->
                    (0 until list.length()).map { list.getString(it) }
                }.orEmpty(),
            )
        }
    }.getOrDefault(emptyList())

    private fun newId() = UUID.randomUUID().toString().take(8)

    companion object {
        const val ALL_ID = "all"
        private const val KEY_FOLDERS = "folders"

        val CATEGORIES = listOf(
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

    /** Живая папка «Сейчас»: собирается из привычки этого часа. */
    const val NOW = "now"

    const val SOCIAL = "social"
    const val MUSIC = "music"
    const val VIDEO = "video"
    const val PHOTO = "photo"
    const val WORK = "work"
    const val GAMES = "games"
    const val MAPS = "maps"

    @StringRes
    fun resOf(key: String): Int = when (key) {
        NOW -> R.string.folder_now
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
