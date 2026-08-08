package app.plein.data

/**
 * Кадры, которые лежат в сборке.
 *
 * Пока это четыре фотографии с Unsplash: две светлые и две тёмные, чтобы
 * подбор под тему работал без сети. Загрузка из фотобанка встанет сюда же.
 */
data class Backdrop(
    val asset: String,
    val author: String,
    val luminance: Float,
)

object Backdrops {

    val bundled = listOf(
        Backdrop("backdrop_fjord.jpg", "Alexey Topolyanskiy", 0.57f),
        Backdrop("backdrop_sunset.jpg", "Kenneth Thewissen", 0.51f),
        Backdrop("backdrop_forest.jpg", "James Forbes", 0.31f),
        Backdrop("backdrop_street.jpg", "Matthew Skinner", 0.19f),
    )

    private const val DARK_LIMIT = 0.42f

    /** Тёмная тема просит тёмные кадры: иначе часы тонут в светлом небе. */
    fun next(current: Backdrop?, dark: Boolean): Backdrop {
        val fitting = bundled.filter { if (dark) it.luminance <= DARK_LIMIT else it.luminance > DARK_LIMIT }
        val pool = fitting.ifEmpty { bundled }
        val position = pool.indexOf(current)
        return pool[(position + 1).mod(pool.size)]
    }

    fun firstFor(dark: Boolean): Backdrop = next(null, dark)
}
