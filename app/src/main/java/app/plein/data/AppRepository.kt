package app.plein.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/**
 * Список приложений и их значки.
 *
 * Через LauncherApps, а не PackageManager: он один умеет рабочий профиль,
 * второго пользователя и присылает события установки без опроса.
 */
class AppRepository(private val context: Context) {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val labels = context.getSharedPreferences("plein_labels", Context.MODE_PRIVATE)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    /** Значки держим в памяти: без кэша шторка мигает на каждом открытии. */
    private val iconCache = LruCache<String, ImageBitmap>(512)

    /** И на диске: память не переживает убийство процесса, а отрисовка дорога. */
    private val diskCache = IconDiskCache(context)

    /**
     *LauncherActivityInfo по ключу.
     *
     * Без этой карты каждый значок заново дёргал getActivityList: сто иконок
     * означали сто обращений к системной службе и пропущенные кадры.
     */
    private val activityInfos = HashMap<String, android.content.pm.LauncherActivityInfo>()

    /** Порядок букв берём у языка интерфейса, а не у русского алфавита всегда. */
    private val collator: Collator = Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
    }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = forget(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = forget(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = forget(packageName)
        override fun onPackagesAvailable(names: Array<out String>, user: UserHandle, replacing: Boolean) = refreshBlocking()
        override fun onPackagesUnavailable(names: Array<out String>, user: UserHandle, replacing: Boolean) = refreshBlocking()
    }

    fun start() = launcherApps.registerCallback(callback)

    fun stop() = launcherApps.unregisterCallback(callback)

    /**
     * Приложение обновилось — старая картинка врёт.
     *
     * Чистим оба кэша по имени пакета: ключ значка начинается с компонента,
     * поэтому в памяти хватает отбора по префиксу.
     */
    private fun forget(packageName: String) {
        val prefix = "$packageName/"
        iconCache.snapshot().keys.forEach { key -> if (key.startsWith(prefix)) iconCache.remove(key) }
        diskCache.forget(packageName)
        refreshBlocking()
    }

    suspend fun refresh() = withContext(Dispatchers.IO) { refreshBlocking() }

    private fun refreshBlocking() {
        val users = buildList {
            add(Process.myUserHandle())
            launcherApps.profiles.forEach { if (it != Process.myUserHandle()) add(it) }
        }
        val pm = context.packageManager
        // Одним вызовом вместо getApplicationInfo на каждый пакет.
        val installed = runCatching { pm.getInstalledApplications(0) }.getOrDefault(emptyList())
        val categories = installed.associate { it.packageName to it.category }
        // Системное не удаляется, поэтому и пункт в меню показывать незачем.
        val systemFlags = installed.associate {
            it.packageName to (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0)
        }

        val infos = HashMap<String, android.content.pm.LauncherActivityInfo>()
        val list = users.flatMap { user ->
            runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
                .map { info ->
                    infos["${info.componentName.flattenToShortString()}#${user.hashCode()}"] = info
                    AppEntry(
                        label = info.label.toString(),
                        component = info.componentName,
                        user = user,
                        category = categories[info.componentName.packageName]
                            ?: android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED,
                        system = systemFlags[info.componentName.packageName] ?: false,
                        customLabel = labels.getString(
                            "${info.componentName.flattenToShortString()}#${user.hashCode()}", null
                        ),
                    )
                }
        }.sortedWith { a, b -> collator.compare(a.title, b.title) }

        synchronized(activityInfos) {
            activityInfos.clear()
            activityInfos.putAll(infos)
        }
        _apps.value = list
    }

    /** Готовый значок из памяти: без корутины, чтобы прокрутка не ждала диспетчер. */
    fun cachedIcon(entry: AppEntry, sizePx: Int, shapeKey: String, iconPack: String = ""): ImageBitmap? =
        iconCache.get(cacheKeyOf(entry, sizePx, shapeKey) + "@" + iconPack)

    private fun cacheKeyOf(entry: AppEntry, sizePx: Int, shapeKey: String) =
        "${entry.key}@$sizePx@$shapeKey"

    private val iconPacks = IconPacks(context)

    fun installedIconPacks(): List<IconPacks.Pack> = iconPacks.installed()

    suspend fun icon(
        entry: AppEntry,
        sizePx: Int,
        shapeKey: String,
        shapePath: android.graphics.Path?,
        iconPack: String = "",
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = cacheKeyOf(entry, sizePx, shapeKey) + "@" + iconPack
        iconCache.get(cacheKey)?.let { return@withContext it }

        val packageName = entry.component.packageName
        diskCache.read(packageName, cacheKey)?.let { stored ->
            val restored = stored.asImageBitmap()
            iconCache.put(cacheKey, restored)
            return@withContext restored
        }

        // Пак имеет приоритет: если для приложения там есть картинка, берём её.
        val fromPack = if (iconPack.isEmpty()) null else iconPacks.icon(iconPack, entry.component)
        val info = synchronized(activityInfos) { activityInfos[entry.key] }
        val drawable: Drawable = fromPack ?: runCatching {
            info?.getIcon(context.resources.displayMetrics.densityDpi)
        }.getOrNull() ?: return@withContext null

        val rendered = renderIcon(drawable, sizePx, shapePath)
        diskCache.write(packageName, cacheKey, rendered)
        val bitmap = rendered.asImageBitmap()
        iconCache.put(cacheKey, bitmap)
        bitmap
    }

    /**
     * Рисуем значок без системной маски: форму задаёт лаунчер, а не прошивка.
     *
     * У адаптивной иконки слои занимают 108 единиц при безопасной зоне 72,
     * поэтому рисуем их в полтора размера и берём центр. Обычная иконка
     * ужимается и кладётся по центру, иначе квадратный логотип упрётся в края.
     */
    private fun renderIcon(drawable: Drawable, sizePx: Int, shapePath: android.graphics.Path?): Bitmap {
        val output = createBitmap(sizePx, sizePx)
        val canvas = Canvas(output)
        // Форму вжигаем в битмап: на экране остаётся обычная картинка без клипа.
        shapePath?.let { canvas.clipPath(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val full = (sizePx * 1.5f).toInt()
            val offset = ((full - sizePx) / 2f).toInt()
            val layers = listOfNotNull(drawable.background, drawable.foreground)
            layers.forEach { layer ->
                layer.setBounds(-offset, -offset, full - offset, full - offset)
                layer.draw(canvas)
            }
        } else {
            val inset = (sizePx * 0.14f).toInt()
            drawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)
            drawable.draw(canvas)
        }
        return output
    }

    /** Своё имя приложения на экране. Пустая строка возвращает системное. */
    fun setCustomLabel(key: String, label: String) {
        val trimmed = label.trim()
        labels.edit().apply {
            if (trimmed.isEmpty()) remove(key) else putString(key, trimmed)
        }.apply()
        _apps.value = _apps.value.map {
            if (it.key == key) it.copy(customLabel = trimmed.ifEmpty { null }) else it
        }
    }

    /**
     * Прогрев значков.
     *
     * Лениво по одному значку означало рывок на каждой новой строке сетки,
     * поэтому после обновления списка кэш заполняется целиком в несколько потоков.
     */
    suspend fun preloadIcons(
        sizePx: Int,
        shapeKey: String,
        shapePath: android.graphics.Path?,
        iconPack: String = "",
    ) = withContext(Dispatchers.IO) {
        val entries = _apps.value
        val gate = Semaphore(4)
        coroutineScope {
            entries.map { entry ->
                async {
                    gate.withPermit { icon(entry, sizePx, shapeKey, shapePath, iconPack) }
                }
            }.awaitAll()
        }
        // Лишние файлы сносим после прогрева: смена формы или сетки оставляет
        // на диске целый мёртвый набор.
        diskCache.trim()
    }

    fun launch(entry: AppEntry) {
        runCatching { launcherApps.startMainActivity(entry.component, entry.user, null, null) }
    }

    /**
     * Удаление: система сама спросит подтверждение.
     *
     * Сперва ACTION_DELETE, затем ACTION_UNINSTALL_PACKAGE: на части прошивок
     * первый интент не разбирается, и без запасного пункт молчал.
     */
    fun uninstall(entry: AppEntry) {
        val uri = android.net.Uri.parse("package:${entry.component.packageName}")
        val attempts = listOf(
            android.content.Intent(android.content.Intent.ACTION_DELETE, uri),
            android.content.Intent(android.content.Intent.ACTION_UNINSTALL_PACKAGE, uri)
                .putExtra(android.content.Intent.EXTRA_RETURN_RESULT, true),
        )
        attempts.forEach { intent ->
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
        // Не вышло удалить — показываем карточку приложения, там кнопка есть всегда.
        openAppInfo(entry)
    }

    fun openAppInfo(entry: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null) }
    }

    fun shortcutsSupported(): Boolean = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    /**
     * Быстрые действия приложения: «Новое письмо», «Позвонить маме».
     *
     * Система отдаёт их только домашнему экрану по умолчанию, у остальных
     * getShortcuts бросает SecurityException. Поэтому пустой список здесь
     * означает и «нет действий», и «мы пока не лаунчер» — раздел просто скрыт.
     */
    suspend fun shortcuts(entry: AppEntry, sizePx: Int): List<AppShortcut> = withContext(Dispatchers.IO) {
        if (!shortcutsSupported()) return@withContext emptyList()

        val query = LauncherApps.ShortcutQuery()
            .setPackage(entry.component.packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        val found = runCatching { launcherApps.getShortcuts(query, entry.user) }
            .getOrNull().orEmpty()

        found
            // У приложения с несколькими значками в меню чужие действия не нужны.
            .filter { it.activity == null || it.activity == entry.component }
            .filter { it.isEnabled }
            // Манифестные идут первыми: их порядок задал разработчик.
            .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
            .take(MAX_SHORTCUTS)
            .map { info ->
                val label = (info.longLabel ?: info.shortLabel ?: "").toString()
                val icon = runCatching {
                    launcherApps.getShortcutIconDrawable(info, context.resources.displayMetrics.densityDpi)
                }.getOrNull()?.let { renderIcon(it, sizePx, circlePath(sizePx)).asImageBitmap() }
                AppShortcut(id = info.id, label = label, icon = icon, info = info)
            }
            .filter { it.label.isNotBlank() }
    }

    fun startShortcut(shortcut: AppShortcut) {
        runCatching {
            launcherApps.startShortcut(shortcut.info, null, null)
        }
    }

    /** Значок действия всегда круглый: форма настроек тут читалась бы как ошибка. */
    private fun circlePath(sizePx: Int): android.graphics.Path =
        android.graphics.Path().apply {
            addCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, android.graphics.Path.Direction.CW)
        }

    companion object {
        private const val MAX_SHORTCUTS = 5

        fun componentOf(pkg: String, cls: String) = ComponentName(pkg, cls)
    }
}

/** Быстрое действие приложения в том виде, в каком его показывает меню. */
class AppShortcut(
    val id: String,
    val label: String,
    val icon: ImageBitmap?,
    internal val info: android.content.pm.ShortcutInfo,
)
