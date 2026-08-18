package app.plein.data

/**
 * Кадры, которые лежат в сборке.
 *
 * Пока это четыре фотографии с Unsplash: две светлые и две тёмные, чтобы
 * подбор под тему работал без сети. Загрузка из фотобанка встанет сюда же.
 */
data class Backdrop(
    val asset: String? = null,
    val file: java.io.File? = null,
    val author: String,
    val credit: String = "$author · Unsplash",
    val luminance: Float,
) {
    /** Ключ для перерисовки: у сетевого кадра это путь файла. */
    val key: String get() = file?.path ?: asset.orEmpty()
}

object Backdrops {

    val bundled = listOf(
        Backdrop(asset = "backdrop_fjord.jpg", author = "Alexey Topolyanskiy", credit = "Alexey Topolyanskiy · Unsplash", luminance = 0.57f),
        Backdrop(asset = "backdrop_sunset.jpg", author = "Kenneth Thewissen", credit = "Kenneth Thewissen · Unsplash", luminance = 0.51f),
        Backdrop(asset = "backdrop_forest.jpg", author = "James Forbes", credit = "James Forbes · Unsplash", luminance = 0.31f),
        Backdrop(asset = "backdrop_street.jpg", author = "Matthew Skinner", credit = "Matthew Skinner · Unsplash", luminance = 0.19f),
    )

    const val DARK_LIMIT = 0.42f

    /** Годится ли кадр текущей теме. */
    fun fits(backdrop: Backdrop, dark: Boolean): Boolean =
        if (dark) backdrop.luminance <= DARK_LIMIT else backdrop.luminance > DARK_LIMIT

    /** Тёмная тема просит тёмные кадры: иначе часы тонут в светлом небе. */
    fun next(current: Backdrop?, dark: Boolean): Backdrop {
        val fitting = bundled.filter { if (dark) it.luminance <= DARK_LIMIT else it.luminance > DARK_LIMIT }
        val pool = fitting.ifEmpty { bundled }
        val position = pool.indexOf(current)
        return pool[(position + 1).mod(pool.size)]
    }

    fun firstFor(dark: Boolean): Backdrop = next(null, dark)

    /**
     * Пора ли идти за новым кадром из-за темы.
     *
     * Смена режима — повод показать новую фотографию, а первый заход после
     * запуска не повод: там уже лежит сохранённый кадр, и дёргать сеть на
     * каждом старте незачем.
     */
    fun themeChanged(was: Boolean?, now: Boolean): Boolean = was != null && was != now
}
