package app.plein.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.plein.ui.icons.IconShape
import app.plein.ui.theme.ThemeMode
import app.plein.ui.theme.Vibrancy

/**
 * Настройки лаунчера.
 *
 * SharedPreferences, а не DataStore: домашний экран читает их до первого кадра,
 * и асинхронное чтение означало бы мигание при каждом запуске.
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

    var themeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(sp.getString(KEY_THEME_MODE, null) ?: ThemeMode.System.name) }
            .getOrDefault(ThemeMode.System)
    )
        private set

    var amoled by mutableStateOf(sp.getBoolean(KEY_AMOLED, false))
        private set

    var dynamicColor by mutableStateOf(sp.getBoolean(KEY_DYNAMIC, false))
        private set

    var vibrancy by mutableStateOf(
        runCatching { Vibrancy.valueOf(sp.getString(KEY_VIBRANCY, null) ?: Vibrancy.Vibrant.name) }
            .getOrDefault(Vibrancy.Vibrant)
    )
        private set

    var seedColor by mutableIntStateOf(sp.getInt(KEY_SEED, DEFAULT_SEED))
        private set

    /** Цвет из кадра перебивает свой seed, пока человек его не выключил. */
    var seedFromPhoto by mutableStateOf(sp.getBoolean(KEY_SEED_FROM_PHOTO, true))
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

    fun updateThemeMode(value: ThemeMode) {
        themeMode = value
        sp.edit().putString(KEY_THEME_MODE, value.name).apply()
    }

    fun updateAmoled(value: Boolean) {
        amoled = value
        sp.edit().putBoolean(KEY_AMOLED, value).apply()
    }

    fun updateDynamicColor(value: Boolean) {
        dynamicColor = value
        sp.edit().putBoolean(KEY_DYNAMIC, value).apply()
    }

    fun updateVibrancy(value: Vibrancy) {
        vibrancy = value
        sp.edit().putString(KEY_VIBRANCY, value.name).apply()
    }

    fun updateSeedColor(argb: Int) {
        seedColor = argb
        seedFromPhoto = false
        sp.edit().putInt(KEY_SEED, argb).putBoolean(KEY_SEED_FROM_PHOTO, false).apply()
    }

    fun updateSeedFromPhoto(value: Boolean) {
        seedFromPhoto = value
        sp.edit().putBoolean(KEY_SEED_FROM_PHOTO, value).apply()
    }

    private companion object {
        const val KEY_SHAPE = "icon_shape"
        const val KEY_COLUMNS = "columns"
        const val KEY_LABELS = "show_labels"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_AMOLED = "amoled"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_VIBRANCY = "vibrancy"
        const val KEY_SEED = "seed_color"
        const val KEY_SEED_FROM_PHOTO = "seed_from_photo"

        /** Амбра из Wickly: тёплая и спокойная в обеих темах. */
        const val DEFAULT_SEED = 0xFFC0863E.toInt()
    }
}

/** Палитра пресетов из Wickly: восемь цветов, из каждого строится вся схема. */
val SeedPresets = listOf(
    0xFFC0863E.toInt(), // амбра
    0xFF9C4368.toInt(), // слива
    0xFF1D9AA4.toInt(), // бирюза
    0xFF2E7D5B.toInt(), // лес
    0xFF5A57C0.toInt(), // индиго
    0xFFB0526A.toInt(), // роза
    0xFFC25B3A.toInt(), // терракота
    0xFF4F7A3A.toInt(), // олива
)
