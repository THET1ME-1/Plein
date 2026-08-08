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

    // ── Часы на кадре ──

    var clockSize by mutableStateOf(sp.getString(KEY_CLOCK_SIZE, null) ?: "m")
        private set

    var clockTwentyFour by mutableStateOf(sp.getBoolean(KEY_CLOCK_24, true))
        private set

    var showDate by mutableStateOf(sp.getBoolean(KEY_SHOW_DATE, true))
        private set

    var showWeather by mutableStateOf(sp.getBoolean(KEY_SHOW_WEATHER, false))
        private set

    /** Имя семейства из каталога Google Fonts. Пусто означает шрифт ДНК. */
    var clockFont by mutableStateOf(sp.getString(KEY_CLOCK_FONT, null).orEmpty())
        private set

    var interfaceFont by mutableStateOf(sp.getString(KEY_UI_FONT, null).orEmpty())
        private set

    /** Пакет выбранного пака значков. Пусто означает системные иконки. */
    var iconPack by mutableStateOf(sp.getString(KEY_ICON_PACK, null).orEmpty())
        private set

    /** open-meteo или met.no: оба без ключа и со свободной лицензией. */
    var weatherProvider by mutableStateOf(sp.getString(KEY_WEATHER_PROVIDER, null) ?: "open-meteo")
        private set

    fun updateWeatherProvider(value: String) {
        weatherProvider = value
        sp.edit().putString(KEY_WEATHER_PROVIDER, value).apply()
    }

    /** Что открывать по нажатию на погоду. Пусто означает ничего. */
    var weatherApp by mutableStateOf(sp.getString(KEY_WEATHER_APP, null).orEmpty())
        private set

    /** dots, bar, numbers или none. */
    var pageIndicator by mutableStateOf(sp.getString(KEY_PAGE_INDICATOR, null) ?: "dots")
        private set

    /** Код языка или пусто для системного. */
    var language by mutableStateOf(sp.getString(KEY_LANGUAGE, null).orEmpty())
        private set

    fun updateLanguage(value: String) {
        language = value
        sp.edit().putString(KEY_LANGUAGE, value).apply()
    }

    fun updateWeatherApp(value: String) {
        weatherApp = value
        sp.edit().putString(KEY_WEATHER_APP, value).apply()
    }

    fun updatePageIndicator(value: String) {
        pageIndicator = value
        sp.edit().putString(KEY_PAGE_INDICATOR, value).apply()
    }

    fun updateIconPack(value: String) {
        iconPack = value
        sp.edit().putString(KEY_ICON_PACK, value).apply()
    }

    fun updateClockSize(value: String) {
        clockSize = value
        sp.edit().putString(KEY_CLOCK_SIZE, value).apply()
    }

    fun updateClockTwentyFour(value: Boolean) {
        clockTwentyFour = value
        sp.edit().putBoolean(KEY_CLOCK_24, value).apply()
    }

    fun updateShowDate(value: Boolean) {
        showDate = value
        sp.edit().putBoolean(KEY_SHOW_DATE, value).apply()
    }

    fun updateShowWeather(value: Boolean) {
        showWeather = value
        sp.edit().putBoolean(KEY_SHOW_WEATHER, value).apply()
    }

    fun updateClockFont(value: String) {
        clockFont = value
        sp.edit().putString(KEY_CLOCK_FONT, value).apply()
    }

    fun updateInterfaceFont(value: String) {
        interfaceFont = value
        sp.edit().putString(KEY_UI_FONT, value).apply()
    }

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
        const val KEY_CLOCK_SIZE = "clock_size"
        const val KEY_CLOCK_24 = "clock_24"
        const val KEY_SHOW_DATE = "clock_show_date"
        const val KEY_SHOW_WEATHER = "clock_show_weather"
        const val KEY_CLOCK_FONT = "clock_font"
        const val KEY_UI_FONT = "ui_font"
        const val KEY_ICON_PACK = "icon_pack"
        const val KEY_WEATHER_PROVIDER = "weather_provider"
        const val KEY_WEATHER_APP = "weather_app"
        const val KEY_LANGUAGE = "language"
        const val KEY_PAGE_INDICATOR = "page_indicator"

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
