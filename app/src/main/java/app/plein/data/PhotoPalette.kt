package app.plein.data

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.scale

/**
 * Цвет темы из кадра.
 *
 * Считаем гистограмму оттенков по насыщенным пикселям и берём самую населённую
 * корзину: так палитра цепляется за то, что человек и называет цветом фотографии,
 * а не за серое небо, которого по площади всегда больше.
 */
object PhotoPalette {

    private const val SAMPLE_WIDTH = 48
    private const val SAMPLE_HEIGHT = 96
    private const val BUCKETS = 24
    private const val MIN_SATURATION = 0.22f
    private const val MIN_VALUE = 0.15f

    fun seedFrom(bitmap: Bitmap): Color {
        val small = bitmap.scale(SAMPLE_WIDTH, SAMPLE_HEIGHT)
        val pixels = IntArray(SAMPLE_WIDTH * SAMPLE_HEIGHT)
        small.getPixels(pixels, 0, SAMPLE_WIDTH, 0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT)

        val sumR = LongArray(BUCKETS)
        val sumG = LongArray(BUCKETS)
        val sumB = LongArray(BUCKETS)
        val count = IntArray(BUCKETS)
        val hsv = FloatArray(3)

        pixels.forEach { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            if (hsv[1] < MIN_SATURATION || hsv[2] < MIN_VALUE) return@forEach
            val bucket = ((hsv[0] / 360f) * BUCKETS).toInt().coerceIn(0, BUCKETS - 1)
            sumR[bucket] += r
            sumG[bucket] += g
            sumB[bucket] += b
            count[bucket]++
        }

        val best = count.indices.maxByOrNull { count[it] } ?: return FallbackSeed
        if (count[best] == 0) return FallbackSeed

        val n = count[best]
        return Color(
            red = (sumR[best] / n).toInt() / 255f,
            green = (sumG[best] / n).toInt() / 255f,
            blue = (sumB[best] / n).toInt() / 255f,
        )
    }

    /** Кадр без единого насыщенного пикселя: снег, туман, ночь. */
    private val FallbackSeed = Color(0xFF2E5D73)
}
