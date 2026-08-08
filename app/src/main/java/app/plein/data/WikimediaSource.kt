package app.plein.data

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Кадры из Викисклада.
 *
 * Ключа не просит и счётчиков лимита не выставляет — в отличие от Openverse,
 * который анонимно даёт двадцать запросов в минуту и замолкает. Взамен требует
 * честный User-Agent со ссылкой на проект, это их правило для всех клиентов.
 *
 * Второе преимущество важнее первого: сервер сам отдаёт превью нужной ширины.
 * Не надо тянуть оригинал на тридцать мегапикселей, чтобы показать его на
 * экране в тысячу точек.
 */
class WikimediaSource(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "backdrops").apply { mkdirs() }
    private val seen = mutableSetOf<String>()
    private val pool = ArrayDeque<Shot>()

    var lastFailure: String? = null
        private set

    data class Shot(
        val id: String,
        val url: String,
        val width: Int,
        val height: Int,
        val author: String,
        val license: String,
    )

    suspend fun next(
        dark: Boolean,
        categories: List<String> = LANDSCAPES,
        screenWidth: Int = 1080,
        onProgress: (Float) -> Unit = {},
    ): Backdrop? = withContext(Dispatchers.IO) {
        var spare: Backdrop? = null
        var lastError: String? = null

        repeat(3) {
            if (pool.isEmpty()) {
                runCatching { search(categories.random(), screenWidth) }
                    .onFailure { lastError = it.reason() }
                    .getOrDefault(emptyList())
                    .let { pool.addAll(it.shuffled()) }
            }
            while (pool.isNotEmpty()) {
                val shot = pool.removeFirst()
                if (shot.id in seen) continue
                val file = runCatching { download(shot, onProgress) }
                    .onFailure { lastError = it.reason() }
                    .getOrNull() ?: continue
                val luminance = luminanceOf(file) ?: continue
                val found = Backdrop(
                    file = file,
                    author = shot.author,
                    credit = listOf(shot.author, shot.license, "Wikimedia").filter { it.isNotBlank() }
                        .joinToString(" · "),
                    luminance = luminance,
                )
                val fits = if (dark) luminance <= 0.5f else luminance > 0.3f
                if (!fits) {
                    if (spare == null) spare = found
                    continue
                }
                seen += shot.id
                lastFailure = null
                return@withContext found
            }
        }
        if (spare == null && seen.size > 120) {
            // Всё показанное помнить незачем: иначе подборка когда-нибудь
            // кончится и лаунчер станет отвечать «ничего не отдал».
            seen.clear()
        }
        lastFailure = if (spare == null) lastError ?: "Викисклад ничего не отдал" else null
        spare
    }

    /**
     * Список снимков из категории.
     *
     * Срез берём со случайной буквы алфавита: без неё выдача одна и та же от
     * запроса к запросу, и как только показаны все, категория молчит навсегда.
     * Берём сразу пятьсот — в категориях по две-три сотни файлов и больше,
     * а полсотни кончались за вечер.
     */
    private fun search(category: String, screenWidth: Int): List<Shot> {
        val width = (screenWidth * 1.4f).toInt().coerceIn(720, 2000)
        val from = ALPHABET.random()
        val url = URL(
            "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
                "&generator=categorymembers&gcmtitle=" + URLEncoder.encode(category, "UTF-8") +
                // Пятьсот за раз: это потолок для обычного клиента, ответ
                // весит около двухсот килобайт и заменяет десяток запросов.
                "&gcmtype=file&gcmlimit=500&gcmstartsortkeyprefix=$from" +
                "&prop=imageinfo&iiprop=url%7Cextmetadata%7Csize&iiurlwidth=$width"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 12000
            setRequestProperty("User-Agent", USER_AGENT)
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()

        val pages = JSONObject(body).optJSONObject("query")?.optJSONObject("pages")
            ?: return emptyList()

        return pages.keys().asSequence().mapNotNull { key ->
            val page = pages.optJSONObject(key) ?: return@mapNotNull null
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: return@mapNotNull null
            val link = info.text("thumburl").ifBlank { info.text("url") }
            if (link.isBlank()) return@mapNotNull null

            val shotWidth = info.optInt("thumbwidth", info.optInt("width"))
            val shotHeight = info.optInt("thumbheight", info.optInt("height"))
            if (!fitsScreen(shotWidth, shotHeight)) return@mapNotNull null

            val meta = info.optJSONObject("extmetadata")
            Shot(
                id = page.text("title"),
                url = link,
                width = shotWidth,
                height = shotHeight,
                author = plainText(meta?.optJSONObject("Artist")?.text("value").orEmpty()),
                license = plainText(meta?.optJSONObject("LicenseShortName")?.text("value").orEmpty()),
            )
        }.toList()
    }

    private fun download(shot: Shot, onProgress: (Float) -> Unit): File {
        val target = File(cacheDir, "wm-" + shot.id.hashCode().toString(16) + ".jpg")
        if (target.exists() && target.length() > 0) return target

        val connection = (URL(shot.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 25000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        val total = connection.contentLength.toLong()
        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(16 * 1024)
                var done = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    done += read
                    if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        connection.disconnect()
        trim()
        return target
    }

    private fun luminanceOf(file: File): Float? {
        val options = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        if (pixels.isEmpty()) return null
        val sum = pixels.sumOf { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.2126 * r + 0.7152 * g + 0.0722 * b)
        }
        return (sum / pixels.size / 255.0).toFloat()
    }

    private fun trim() {
        val files = cacheDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(24).forEach { it.delete() }
    }

    private fun Throwable.reason(): String = when (this) {
        is java.net.SocketTimeoutException -> "сеть не отвечает"
        is java.net.UnknownHostException -> "нет доступа к сети"
        else -> this::class.simpleName.orEmpty().ifEmpty { "сбой сети" }
    }

    companion object {
        /** Правило Викисклада: клиент обязан представиться и оставить ссылку. */
        const val USER_AGENT = "PleinLauncher/0.3 (https://github.com/THET1ME-1/Plein)"

        /**
         * Годится ли кадр шапке.
         *
         * Шапка — лежачая полоса во всю ширину экрана, поэтому обычные
         * горизонтальные снимки как раз то, что нужно. Прошлая проверка
         * пропускала только стоячие, а их в подборках три из сорока пяти:
         * почти всё выбрасывалось, и категория пустела с третьего кадра.
         * Отсекаем только крайности — панорамы и башни.
         */
        fun fitsScreen(width: Int, height: Int): Boolean {
            if (width <= 0 || height <= 0) return false
            val ratio = height.toFloat() / width
            return ratio in 0.35f..2.0f
        }

        /** Буквы для случайного среза по алфавиту. */
        private val ALPHABET = ('a'..'z').toList() + ('0'..'9').toList()

        /**
         * Отобранные людьми категории: там снимки, а не документы и схемы.
         *
         * Числа рядом — сколько файлов в категории на август 2026. Мелкие
         * выброшены: «Quality images of skies» и «of seas» оказались пустыми,
         * «of nature» — тринадцать файлов, на них подборка кончалась сразу.
         */
        val LANDSCAPES = listOf(
            "Category:Quality images of nature",              // 1094
            "Category:Quality images of landscapes",          // 566
            "Category:Quality images of valleys",             // 552
            "Category:Featured pictures of cityscapes",       // 519
            "Category:Featured pictures of landscapes",       // 512
            "Category:Quality images of sunsets",             // 511
            "Category:Quality images of lakes",               // 356
            "Category:Quality images of cityscapes",          // 239
            "Category:Quality images of mountains",           // 225
            "Category:Quality images of waterfalls",          // 173
            "Category:Featured pictures of mountains",        // 158
        )

        /**
         * Подпись автора приходит куском HTML со ссылками.
         *
         * Тянуть в лаунчер разбор HTML незачем: убираем теги, разворачиваем
         * несколько сущностей и режем длину — на экране всё равно одна строка.
         */
        fun plainText(html: String): String = html
            .replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(40)
    }
}
