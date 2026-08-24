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
    private val wikimedia: WikimediaSource,
    private val screenWidth: Int = 1080,
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
            BackdropOrigin.Wikimedia -> fromWikimedia(dark, weatherCode, onProgress)
            BackdropOrigin.Openverse -> fromNetwork(dark, weatherCode, onProgress)
        }
        if (fresh == null && lastFailure == null) {
            lastFailure = when (prefs.backdropOrigin) {
                BackdropOrigin.Gallery -> "своих снимков нет"
                BackdropOrigin.Folder -> "в папке нет картинок"
                BackdropOrigin.Wikimedia -> wikimedia.lastFailure ?: "Викисклад не ответил"
                BackdropOrigin.Openverse -> source.lastFailure ?: "фотобанк не ответил"
            }
        }
        return fresh ?: Backdrops.next(current, dark)
    }

    /**
     * Кадр с Викисклада.
     *
     * Ключа и лимитов там нет, поэтому и хитрить с запасом запросов не нужно —
     * достаточно вежливо представиться.
     */
    private suspend fun fromWikimedia(
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

        // Своя тема ищется словами, а не по категориям: категорий на такие
        // слова на Викискладе просто нет.
        val theme = themeQueries(weatherCode)
        if (theme != null) {
            val found = wikimedia.next(
                dark = dark,
                screenWidth = screenWidth,
                search = theme.withHint,
                onProgress = onProgress,
            ) ?: wikimedia.next(
                dark = dark,
                screenWidth = screenWidth,
                search = theme.plain,
                onProgress = onProgress,
            )
            if (found != null) return found
            // Викисклад по теме молчит — за словами идём в Openverse, а не во
            // вшитые кадры: тема человеку важнее источника.
            return source.next(dark, theme.withHint, onProgress)
                ?: source.next(dark, theme.plain, onProgress)
        }

        // Викисклад молчит — не сдаёмся во вшитые кадры, идём в Openverse.
        return wikimedia.next(dark = dark, screenWidth = screenWidth, onProgress = onProgress)
            ?: source.next(dark, onProgress = onProgress)
    }

    /** Запросы своей темы: с подсказкой погоды или времени и без неё. */
    private fun themeQueries(weatherCode: Int?): ThemeQueries? {
        val words = BackdropQueries.themeWords(prefs.backdropTheme)
        if (words.isEmpty()) return null

        // Погода важнее времени: дождь за окном виден, а вечер читается по свету.
        val hint = (if (prefs.backdropByWeather && weatherCode != null) {
            BackdropQueries.weatherWord(weatherCode)
        } else {
            null
        }) ?: if (prefs.backdropByTime) BackdropQueries.timeWord(DayPart.now()) else null

        return ThemeQueries(withHint = BackdropQueries.combine(words, hint), plain = words)
    }

    /**
     * Тема с подсказкой и она же голая.
     *
     * Сначала спрашиваем «sea rain», а если по такой паре пусто — просто «sea»:
     * кадр не совсем про сегодняшнюю погоду лучше, чем вшитый.
     */
    private class ThemeQueries(val withHint: List<String>, val plain: List<String>)

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

        // Своя тема ведёт запрос: к ней приклеивается погода или время суток,
        // а если по такой паре фотобанк молчит — ищем по одной теме.
        themeQueries(weatherCode)?.let { theme ->
            return source.next(dark, theme.withHint, onProgress)
                ?: source.next(dark, theme.plain, onProgress)
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
