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
     * Новый кадр под тему.
     *
     * Тему подбираем яркостью самой фотографии: сервис такого фильтра не даёт,
     * поэтому качаем кандидатов и меряем сами. Уже показанные пропускаем,
     * иначе кнопка начнёт возвращать одно и то же.
     */
    suspend fun next(
        dark: Boolean,
        queries: List<String> = QUERIES,
    ): Backdrop? = withContext(Dispatchers.IO) {
        repeat(3) {
            val candidates = runCatching { search(queries.random(), randomPage()) }.getOrDefault(emptyList())
            candidates.shuffled().forEach { candidate ->
                if (candidate.id in seen) return@forEach
                val file = runCatching { download(candidate) }.getOrNull() ?: return@forEach
                val luminance = luminanceOf(file) ?: return@forEach
                val fits = if (dark) luminance <= 0.45f else luminance > 0.38f
                if (!fits) {
                    file.delete()
                    return@forEach
                }
                seen += candidate.id
                return@withContext Backdrop(
                    file = file,
                    author = candidate.author,
                    credit = candidate.credit,
                    luminance = luminance,
                )
            }
        }
        null
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
            val creator = item.optString("creator").ifBlank { "Unknown" }
            val license = item.optString("license").uppercase()
            Candidate(
                id = item.optString("id"),
                url = link,
                author = creator,
                credit = "$creator · $license · Openverse",
            )
        }
    }

    private fun download(candidate: Candidate): File {
        val target = File(cacheDir, candidate.id.take(24) + ".jpg")
        if (target.exists() && target.length() > 0) return target

        val connection = (URL(candidate.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 20000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        connection.use { open ->
            open.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
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
