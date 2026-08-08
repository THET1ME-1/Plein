package app.plein.search

import android.content.Context
import android.content.Intent
import android.provider.Settings
import app.plein.R
import java.util.Locale

/**
 * Экраны системных настроек в поиске.
 *
 * Своих названий у системы не спросить: список экранов известен, а вот как он
 * называется на языке человека — нет. Поэтому названия свои и переведены
 * вместе с остальным интерфейсом.
 */
object SystemSettings {

    data class Entry(val titleRes: Int, val action: String)

    val all = listOf(
        Entry(R.string.setting_wifi, Settings.ACTION_WIFI_SETTINGS),
        Entry(R.string.setting_bluetooth, Settings.ACTION_BLUETOOTH_SETTINGS),
        Entry(R.string.setting_sound, Settings.ACTION_SOUND_SETTINGS),
        Entry(R.string.setting_display, Settings.ACTION_DISPLAY_SETTINGS),
        Entry(R.string.setting_battery, Intent.ACTION_POWER_USAGE_SUMMARY),
        Entry(R.string.setting_apps, Settings.ACTION_APPLICATION_SETTINGS),
        Entry(R.string.setting_storage, Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        Entry(R.string.setting_security, Settings.ACTION_SECURITY_SETTINGS),
        Entry(R.string.setting_location, Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        Entry(R.string.setting_date, Settings.ACTION_DATE_SETTINGS),
        Entry(R.string.setting_language, Settings.ACTION_LOCALE_SETTINGS),
        Entry(R.string.setting_accessibility, Settings.ACTION_ACCESSIBILITY_SETTINGS),
        Entry(R.string.setting_developer, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        Entry(R.string.setting_data, Settings.ACTION_DATA_ROAMING_SETTINGS),
        Entry(R.string.setting_notifications, "android.settings.NOTIFICATION_SETTINGS"),
        Entry(R.string.setting_dream, Settings.ACTION_DREAM_SETTINGS),
        Entry(R.string.setting_home, Settings.ACTION_HOME_SETTINGS),
        Entry(R.string.setting_privacy, Settings.ACTION_PRIVACY_SETTINGS),
        Entry(R.string.setting_about, Settings.ACTION_DEVICE_INFO_SETTINGS),
    )

    /** Поиск по названию: по началу слова, чтобы «зву» находило «Звук». */
    fun search(context: Context, query: String): List<Entry> {
        val needle = query.trim().lowercase(Locale.getDefault())
        if (needle.length < 2) return emptyList()
        return all.filter { entry ->
            val title = context.getString(entry.titleRes).lowercase(Locale.getDefault())
            title.startsWith(needle) || title.split(' ').any { it.startsWith(needle) }
        }
    }

    fun open(context: Context, entry: Entry): Boolean = runCatching {
        context.startActivity(Intent(entry.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
