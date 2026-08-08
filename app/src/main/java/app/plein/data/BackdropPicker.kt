package app.plein.data

import android.content.Context

/**
 * Кто выбирает следующий кадр.
 *
 * Экран знает только «дай новый», а откуда он придёт — из своей папки, из
 * фотобанка под погоду или из вшитых — решается здесь. Иначе домашний экран
 * оброс бы разбором источников и правил про трафик.
 */
class BackdropPicker(
    private val context: Context,
    private val prefs: Prefs,
    private val library: BackdropLibrary,
    private val source: BackdropSource,
) {

    /** Почему на экране остался прежний или вшитый кадр. */
    var lastFailure: String? = null
        private set

    suspend fun next(
        current: Backdrop,
        dark: Boolean,
        weatherCode: Int?,
        onProgress: (Float) -> Unit = {},
    ): Backdrop {
        lastFailure = null
        val fresh = when (prefs.backdropOrigin) {
            BackdropOrigin.Gallery -> library.fromOwn(current)
            BackdropOrigin.Folder -> prefs.backdropFolder
                .takeIf { it.isNotEmpty() }
                ?.let { library.fromFolder(it, current) }
            BackdropOrigin.Openverse -> fromNetwork(dark, weatherCode, onProgress)
        }
        if (fresh == null && lastFailure == null) {
            lastFailure = when (prefs.backdropOrigin) {
                BackdropOrigin.Gallery -> "своих снимков нет"
                BackdropOrigin.Folder -> "в папке нет картинок"
                BackdropOrigin.Openverse -> source.lastFailure ?: "фотобанк не ответил"
            }
        }
        return fresh ?: Backdrops.next(current, dark)
    }

    /**
     * Кадр из фотобанка.
     *
     * Слова запроса складываются из погоды и времени суток: погода важнее,
     * потому что дождь за окном виден, а вечер и так читается по свету.
     */
    private suspend fun fromNetwork(
        dark: Boolean,
        weatherCode: Int?,
        onProgress: (Float) -> Unit,
    ): Backdrop? {
        if (!Network.isOnline(context)) {
            lastFailure = "нет сети"
            return null
        }
        if (prefs.backdropWifiOnly && Network.isCellular(context)) {
            lastFailure = "включено «только Wi-Fi», а сеть мобильная"
            return null
        }

        val byWeather = if (prefs.backdropByWeather && weatherCode != null) {
            BackdropQueries.forWeather(weatherCode)
        } else {
            null
        }
        val byTime = if (prefs.backdropByTime) BackdropQueries.forTime(DayPart.now()) else null
        val queries = byWeather ?: byTime
        return if (queries == null) source.next(dark, onProgress = onProgress)
        else source.next(dark, queries, onProgress)
    }
}
