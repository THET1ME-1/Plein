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

    suspend fun next(
        current: Backdrop,
        dark: Boolean,
        weatherCode: Int?,
        onProgress: (Float) -> Unit = {},
    ): Backdrop {
        val fresh = when (prefs.backdropOrigin) {
            BackdropOrigin.Gallery -> library.fromOwn(current)
            BackdropOrigin.Folder -> prefs.backdropFolder
                .takeIf { it.isNotEmpty() }
                ?.let { library.fromFolder(it, current) }
            BackdropOrigin.Openverse -> fromNetwork(dark, weatherCode, onProgress)
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
        if (!Network.isOnline(context)) return null
        if (prefs.backdropWifiOnly && Network.isCellular(context)) return null

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
