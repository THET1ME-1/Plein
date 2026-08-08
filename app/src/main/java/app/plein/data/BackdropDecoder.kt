package app.plein.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream

/**
 * Кадр под размер экрана.
 *
 * Фотографии из банка приходят по три-четыре тысячи точек в ширину: в памяти
 * это полсотни мегабайт, а на телефоне — рывки и сборка мусора на каждом
 * движении. Читаем размеры, считаем степень двойки и декодируем сразу мелким.
 *
 * Чтение границ отделено от чтения картинки нарочно: при `inJustDecodeBounds`
 * декодер всегда возвращает null, и на этом легко построить проверку, которая
 * молча выбрасывает любой кадр — экран остаётся пустым, хотя файл на месте.
 */
object BackdropDecoder {

    fun forScreen(context: Context, backdrop: Backdrop, screenWidth: Int): Bitmap? {
        fun open(): InputStream? = when {
            backdrop.file != null -> backdrop.file.inputStream()
            backdrop.asset != null -> context.assets.open(backdrop.asset)
            else -> null
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val probe = open() ?: return null
        probe.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleFor(bounds.outWidth, screenWidth)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val stream = open() ?: return null
        return stream.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** Полтора экрана в запасе: шапка при сворачивании подъезжает зумом. */
    fun sampleFor(sourceWidth: Int, screenWidth: Int): Int {
        val target = (screenWidth * 1.5f).toInt().coerceAtLeast(720)
        var sample = 1
        while (sourceWidth / (sample * 2) >= target) sample *= 2
        return sample
    }
}
