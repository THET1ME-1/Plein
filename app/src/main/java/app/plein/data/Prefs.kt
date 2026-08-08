package app.plein.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.plein.ui.icons.IconShape

/**
 * Настройки лаунчера.
 *
 * SharedPreferences, а не DataStore: домашний экран читает их до первого кадра,
 * и асинхронное чтение здесь означало бы мигание при каждом запуске.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("plein", Context.MODE_PRIVATE)

    var iconShape by mutableStateOf(
        runCatching { IconShape.valueOf(sp.getString(KEY_SHAPE, null) ?: IconShape.Default.name) }
            .getOrDefault(IconShape.Default)
    )
        private set

    var columns by mutableIntStateOf(sp.getInt(KEY_COLUMNS, 4))
        private set

    var showLabels by mutableStateOf(sp.getBoolean(KEY_LABELS, true))
        private set

    var darkTheme by mutableStateOf(sp.getBoolean(KEY_DARK, false))
        private set

    var followSystemTheme by mutableStateOf(sp.getBoolean(KEY_FOLLOW_SYSTEM, true))
        private set

    fun updateIconShape(value: IconShape) {
        iconShape = value
        sp.edit().putString(KEY_SHAPE, value.name).apply()
    }

    fun updateColumns(value: Int) {
        columns = value.coerceIn(3, 6)
        sp.edit().putInt(KEY_COLUMNS, columns).apply()
    }

    fun updateShowLabels(value: Boolean) {
        showLabels = value
        sp.edit().putBoolean(KEY_LABELS, value).apply()
    }

    fun updateDarkTheme(value: Boolean) {
        darkTheme = value
        followSystemTheme = false
        sp.edit().putBoolean(KEY_DARK, value).putBoolean(KEY_FOLLOW_SYSTEM, false).apply()
    }

    fun updateFollowSystemTheme(value: Boolean) {
        followSystemTheme = value
        sp.edit().putBoolean(KEY_FOLLOW_SYSTEM, value).apply()
    }

    private companion object {
        const val KEY_SHAPE = "icon_shape"
        const val KEY_COLUMNS = "columns"
        const val KEY_LABELS = "show_labels"
        const val KEY_DARK = "dark_theme"
        const val KEY_FOLLOW_SYSTEM = "follow_system_theme"
    }
}
