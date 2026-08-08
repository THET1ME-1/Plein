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

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    /** Значки держим в памяти: без кэша шторка мигает на каждом открытии. */
    private val iconCache = LruCache<String, ImageBitmap>(256)

    private val collator: Collator = Collator.getInstance(Locale("ru")).apply {
        strength = Collator.PRIMARY
    }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refreshBlocking()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refreshBlocking()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refreshBlocking()
        override fun onPackagesAvailable(names: Array<out String>, user: UserHandle, replacing: Boolean) = refreshBlocking()
        override fun onPackagesUnavailable(names: Array<out String>, user: UserHandle, replacing: Boolean) = refreshBlocking()
    }

    fun start() = launcherApps.registerCallback(callback)

    fun stop() = launcherApps.unregisterCallback(callback)

    suspend fun refresh() = withContext(Dispatchers.IO) { refreshBlocking() }

    private fun refreshBlocking() {
        val users = buildList {
            add(Process.myUserHandle())
            launcherApps.profiles.forEach { if (it != Process.myUserHandle()) add(it) }
        }
        val pm = context.packageManager
        val list = users.flatMap { user ->
            runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
                .map { info ->
                    AppEntry(
                        label = info.label.toString(),
                        component = info.componentName,
                        user = user,
                        category = runCatching {
                            pm.getApplicationInfo(info.componentName.packageName, 0).category
                        }.getOrDefault(android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED),
                    )
                }
        }.sortedWith { a, b -> collator.compare(a.title, b.title) }

        _apps.value = list
    }

    suspend fun icon(entry: AppEntry, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${entry.key}@$sizePx"
        iconCache.get(cacheKey)?.let { return@withContext it }

        val drawable: Drawable = runCatching {
            launcherApps.getActivityList(entry.component.packageName, entry.user)
                .firstOrNull { it.componentName == entry.component }
                ?.getIcon(context.resources.displayMetrics.densityDpi)
        }.getOrNull() ?: return@withContext null

        val bitmap = renderIcon(drawable, sizePx).asImageBitmap()
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
    private fun renderIcon(drawable: Drawable, sizePx: Int): Bitmap {
        val output = createBitmap(sizePx, sizePx)
        val canvas = Canvas(output)

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

    fun launch(entry: AppEntry) {
        runCatching { launcherApps.startMainActivity(entry.component, entry.user, null, null) }
    }

    fun openAppInfo(entry: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null) }
    }

    fun shortcutsSupported(): Boolean = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    companion object {
        fun componentOf(pkg: String, cls: String) = ComponentName(pkg, cls)
    }
}
