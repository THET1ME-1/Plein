package app.plein.data

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/**
 * Строка из JSON без сюрпризов.
 *
 * `optString` отдаёт «null» строкой, когда поле в JSON равно null: подписи
 * получались вида «null · CC0 · Openverse».
 */
internal fun JSONObject.text(name: String): String {
    if (isNull(name)) return ""
    return optString(name).takeIf { it != "null" }.orEmpty()
}

/**
 * Кадр из открытого фотобанка.
 *
 * Openverse: работает без ключа и отдаёт только материал со свободной
 * лицензией. Unsplash и Pexels в лаунчер не годятся, у обоих в правилах
 * приложения-обои прямо запрещены.
 */
class BackdropSource(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "backdrops").apply { mkdirs() }
    private val seen = mutableSetOf<String>()

    /**
     * Запас кандидатов.
     *
     * У фотобанка лимит: двадцать запросов в минуту и две сотни в сутки. Один
     * поиск отдаёт дюжину снимков, и раньше каждое нажатие тратило до трёх
     * запросов впустую. Теперь выдача складывается сюда, и следующие кадры
     * берутся из запаса, пока он не кончится.
     */
    private val pool = ArrayDeque<Candidate>()

    /**
     * Новый кадр.
     *
     * Яркость — предпочтение, а не пропуск. Раньше кадр светлее или темнее
     * нужного выбрасывался, и на светлой теме не подходил почти никто: пейзажи
     * в основном тёмные, все кандидаты улетали в мусор, а человек видел вшитую
     * фотографию вместо новой. Теперь неподошедший кадр держим про запас и
     * отдаём его, если лучше ничего не нашлось.
     */
    /** Почему кадр не пришёл. Пусто — всё хорошо. */
    var lastFailure: String? = null
        private set

    suspend fun next(
        dark: Boolean,
        queries: List<String> = QUERIES,
        onProgress: (Float) -> Unit = {},
    ): Backdrop? = withContext(Dispatchers.IO) {
        var spare: Backdrop? = null
        var searched = 0
        var downloaded = 0
        var lastError: String? = null

        repeat(3) {
            if (pool.isEmpty()) {
                val candidates = runCatching { search(queries.random(), randomPage()) }
                    .onFailure { lastError = it.shortReason() }
                    .getOrDefault(emptyList())
                pool.addAll(candidates.shuffled())
            }
            val candidates = buildList {
                while (pool.isNotEmpty() && size < 6) add(pool.removeFirst())
            }
            searched += candidates.size
            candidates.forEach { candidate ->
                if (candidate.id in seen) return@forEach
                val file = runCatching { download(candidate, onProgress) }
                    .onFailure { lastError = it.shortReason() }
                    .getOrNull() ?: return@forEach
                downloaded++
                val luminance = luminanceOf(file) ?: return@forEach
                val found = Backdrop(
                    file = file,
                    author = candidate.author,
                    credit = candidate.credit,
                    luminance = luminance,
                )
                val fits = if (dark) luminance <= 0.5f else luminance > 0.3f
                if (!fits) {
                    if (spare == null) spare = found
                    return@forEach
                }
                seen += candidate.id
                lastFailure = null
                return@withContext found
            }
        }
        lastFailure = when {
            spare != null -> null
            searched == 0 -> lastError ?: "поиск ничего не отдал"
            downloaded == 0 -> lastError ?: "снимки не качаются"
            else -> "все кадры уже показаны"
        }
        spare
    }

    private fun Throwable.shortReason(): String = when (this) {
        is java.net.SocketTimeoutException -> "сеть не отвечает"
        is java.net.UnknownHostException -> "нет доступа к сети"
        is java.io.FileNotFoundException -> "фотобанк отказал (лимит запросов)"
        else -> this::class.simpleName.orEmpty().ifEmpty { "сбой сети" }
    }

    private data class Candidate(
        val id: String,
        val url: String,
        val author: String,
        val credit: String,
    )

    private fun randomPage(): Int = Random.nextInt(1, 8)

    private fun search(query: String, page: Int): List<Candidate> {
        val url = URL(
            // category=photograph отсекает карты, схемы и рисунки, из-за которых
            // в выдаче попадались люди и всякий хлам вместо видов.
            "https://api.openverse.org/v1/images/?q=$query&license_type=commercial" +
                "&size=large&category=photograph&aspect_ratio=tall&mature=false" +
                "&page_size=12&page=$page"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 12000
            setRequestProperty("User-Agent", USER_AGENT)
        }
        val body = connection.use { it.inputStream.bufferedReader().readText() }
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()

        return (0 until results.length()).mapNotNull { index ->
            val item = results.getJSONObject(index)
            // url это оригинал, thumbnail мелкий: берём только оригинал.
            val link = item.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val width = item.optInt("width")
            if (width in 1..1000) return@mapNotNull null
            // optString отдаёт строку «null», когда поле в JSON равно null:
            // отсюда и брались подписи вида «null · CC0 · Openverse».
            val creator = item.text("creator").ifBlank { "Unknown" }
            val license = item.text("license").uppercase()
            Candidate(
                id = item.optString("id"),
                url = link,
                author = creator,
                credit = "$creator · $license · Openverse",
            )
        }
    }

    private fun download(candidate: Candidate, onProgress: (Float) -> Unit = {}): File {
        val target = File(cacheDir, candidate.id.take(24) + ".jpg")
        if (target.exists() && target.length() > 0) return target

        val connection = (URL(candidate.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 20000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        connection.use { open ->
            // Сервер не всегда говорит размер: тогда показываем неопределённый
            // ход, а не врём процентами.
            val total = open.contentLength.toLong()
            open.inputStream.use { input ->
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
        }
        trimCache()
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

    /** Кэш не должен расти бесконечно: держим два десятка последних кадров. */
    private fun trimCache() {
        val files = cacheDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(20).forEach { it.delete() }
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }

    private companion object {
        const val USER_AGENT = "PleinLauncher/0.1 (https://github.com/THET1ME-1/Plein)"

        /** Только виды: города и природа, без портретов и репортажа. */
        val QUERIES = listOf(
            "cityscape", "city skyline night", "aerial city view", "old town street",
            "mountain landscape", "misty forest", "autumn forest", "sea sunset",
            "lake reflection", "desert dunes", "northern lights", "waterfall canyon",
            "green valley", "snowy peaks", "coastal cliffs", "field sunrise",
        )
    }
}
