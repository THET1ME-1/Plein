package app.plein.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Полноэкранные жесты на прошивках Xiaomi.
 *
 * HyperOS держит жесты и экран многозадачности внутри своего лаунчера, и как
 * только домашним становится чужой, навигация молча падает на три кнопки.
 * Запрет снимается одним флагом в защищённых настройках — `force_fsg_nav_bar`.
 *
 * Писать туда обычному приложению нельзя, нужно `WRITE_SECURE_SETTINGS`. Его
 * выдают один раз со стороны, командой
 *
 *     adb shell pm grant app.plein android.permission.WRITE_SECURE_SETTINGS
 *
 * причём с самого телефона тоже: беспроводная отладка плюс LADB или Shizuku,
 * компьютер не нужен. После этого лаунчер ставит флаг сам — при запуске, после
 * своего обновления и после перезагрузки телефона, где флаг обнуляется.
 *
 * Без разрешения остаётся ручной путь: открыть скрытый экран выбора режима
 * навигации и переключиться там. Он тоже держится до перезагрузки.
 */
object NavGestures {

    /** Флаг HyperOS: единица разрешает жесты поверх запрета для чужих лаунчеров. */
    private const val KEY = "force_fsg_nav_bar"

    /** Скрытые экраны выбора режима навигации: сначала MIUI, потом стоковый. */
    private val screens = listOf(
        ComponentName(
            "com.android.settings",
            "com.android.settings.Settings\$NavigationModeSettingsActivity",
        ),
        ComponentName(
            "com.android.settings",
            "com.android.settings.Settings\$SystemNavigationGestureSettingsActivity",
        ),
    )

    /** Прошивка вообще знает про этот флаг? На чистом Android его нет. */
    fun supported(context: Context): Boolean =
        runCatching {
            Settings.Global.getString(context.contentResolver, KEY) != null
        }.getOrDefault(false) || screen(context) != null

    fun enabled(context: Context): Boolean =
        runCatching {
            Settings.Global.getInt(context.contentResolver, KEY, 0) == 1
        }.getOrDefault(false)

    /** Разрешение выдаётся снаружи, у самого приложения запросить его нельзя. */
    fun granted(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Поставить флаг, если разрешение есть.
     *
     * Вызывается на каждом запуске и после перезагрузки: HyperOS сбрасывает
     * значение вместе с системными настройками, и без повтора жесты живут
     * только до выключения телефона.
     */
    fun apply(context: Context): Boolean {
        if (!granted(context)) return false
        if (enabled(context)) return true
        return runCatching {
            Settings.Global.putInt(context.contentResolver, KEY, 1)
        }.getOrDefault(false)
    }

    /** Существующий на этой прошивке экран навигации, иначе null. */
    private fun screen(context: Context): ComponentName? =
        screens.firstOrNull { component ->
            val intent = Intent().setComponent(component)
            intent.resolveActivity(context.packageManager) != null
        }

    /**
     * Ручной путь: открыть системный выбор режима навигации.
     *
     * Экран спрятан из меню, но активность открыта, и запуск её разрешений не
     * требует. Если прошивка её не отдаёт, уходим на настройки домашнего
     * экрана — оттуда навигация тоже доступна.
     */
    fun openSystemScreen(context: Context): Boolean {
        val component = screen(context)
        val intent = if (component != null) {
            Intent().setComponent(component)
        } else {
            Intent(Settings.ACTION_HOME_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
