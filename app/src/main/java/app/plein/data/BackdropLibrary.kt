package app.plein.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

/**
 * Откуда лаунчер берёт кадры.
 *
 * По умолчанию — фотобанк: лаунчер про то и есть, что каждый раз новый вид.
 * Вшитые фотографии в этом списке не значатся, они остались запасом на случай,
 * когда сети нет вовсе.
 */
enum class BackdropOrigin {
    /** Openverse: свободные лицензии, ключ не нужен. */
    Openverse,

    /** Снимки, выбранные в галерее и скопированные к себе. */
    Gallery,

    /** Папка на телефоне, выбранная через системный проводник. */
    Folder;

    companion object {
        fun of(name: String?): BackdropOrigin =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Openverse
    }
}

/** Время суток: от него зависит и запрос к фотобанку, и выбор из своей папки. */
enum class DayPart {
    Morning, Day, Evening, Night;

    companion object {
        fun at(hour: Int): DayPart = when (hour) {
            in 5..10 -> Morning
            in 11..16 -> Day
            in 17..21 -> Evening
            else -> Night
        }

        fun now(): DayPart = at(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }
}

/**
 * Кадры со своего телефона.
 *
 * Выбранное в галерее копируем к себе: постоянные права на чужой файл живут
 * до первой чистки, а копия переживает и перезагрузку, и удаление снимка.
 * Папку, наоборот, не копируем — человек кладёт туда что хочет, лаунчер
 * читает по правам на дерево.
 */
class BackdropLibrary(private val context: Context) {

    private val ownDir = File(context.filesDir, "backdrops").apply { mkdirs() }

    /** Скопировать выбранное фото к себе. Возвращает готовый кадр. */
    suspend fun addFromGallery(uri: Uri): Backdrop? = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(ownDir, "own-${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            backdropOf(target)
        }.getOrNull()
    }

    fun ownFiles(): List<File> = ownDir.listFiles()?.filter { it.length() > 0 }.orEmpty()

    fun forget(file: File) {
        file.delete()
    }

    /** Случайный снимок из своей папки, по возможности не тот же самый. */
    suspend fun fromOwn(current: Backdrop?): Backdrop? = withContext(Dispatchers.IO) {
        val files = ownFiles()
        if (files.isEmpty()) return@withContext null
        val pool = files.filterNot { it.path == current?.file?.path }.ifEmpty { files }
        backdropOf(pool.random())
    }

    /**
     * Случайный снимок из папки, выбранной человеком.
     *
     * Читаем через дерево документов, а файл копируем во временный: декодер
     * работает с путём, а не с потоком, и на большом кадре это заметно быстрее.
     */
    suspend fun fromFolder(treeUri: String, current: Backdrop?): Backdrop? = withContext(Dispatchers.IO) {
        runCatching {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return@withContext null
            val images = tree.listFiles().filter { doc ->
                doc.isFile && (doc.type?.startsWith("image/") == true)
            }
            if (images.isEmpty()) return@withContext null

            val cacheDir = File(context.cacheDir, "folder-backdrops").apply { mkdirs() }
            val pick = images.random()
            val target = File(cacheDir, "folder-${pick.name?.hashCode() ?: 0}.jpg")
            if (!target.exists() || target.length() == 0L) {
                context.contentResolver.openInputStream(pick.uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null
            }
            if (target.path == current?.file?.path && images.size > 1) {
                return@withContext fromFolder(treeUri, null)
            }
            backdropOf(target)
        }.getOrNull()
    }

    private fun backdropOf(file: File): Backdrop? {
        val luminance = luminanceOf(file) ?: return null
        return Backdrop(
            file = file,
            author = "",
            credit = "",
            luminance = luminance,
        )
    }

    /** Яркость нужна, чтобы кадр не спорил с темой: светлый под светлую. */
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
}

/**
 * Слова для фотобанка по времени суток и погоде.
 *
 * Держим отдельно от источника: список запросов правится чаще, чем сеть.
 */
object BackdropQueries {

    private val morning = listOf("sunrise mountains", "morning mist forest", "dawn sea", "misty valley sunrise")
    private val day = listOf("cityscape day", "green valley", "coastal cliffs", "desert dunes")
    private val evening = listOf("sea sunset", "golden hour city", "sunset field", "dusk skyline")
    private val night = listOf("city skyline night", "northern lights", "starry sky mountains", "night street rain")

    private val byWeather = mapOf(
        "rain" to listOf("rain window city", "wet street reflections", "rainy forest"),
        "snow" to listOf("snowy peaks", "winter forest snow", "snow city street"),
        "fog" to listOf("misty forest", "foggy hills", "fog city morning"),
        "clear" to listOf("clear sky mountains", "blue sky sea", "sunny dunes"),
        "clouds" to listOf("cloudy hills", "storm clouds field", "overcast coast"),
    )

    fun forTime(part: DayPart): List<String> = when (part) {
        DayPart.Morning -> morning
        DayPart.Day -> day
        DayPart.Evening -> evening
        DayPart.Night -> night
    }

    /** Коды Open-Meteo: 0 ясно, 45 туман, 51–67 дождь, 71–86 снег, 95+ гроза. */
    fun forWeather(code: Int): List<String>? = when (code) {
        0, 1 -> byWeather["clear"]
        2, 3 -> byWeather["clouds"]
        45, 48 -> byWeather["fog"]
        in 51..67, in 80..82, in 95..99 -> byWeather["rain"]
        in 71..77, 85, 86 -> byWeather["snow"]
        else -> null
    }
}
