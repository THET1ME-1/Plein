package app.plein.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.createBitmap

/**
 * Значок одним тоном.
 *
 * Android 13 отдаёт у адаптивной иконки готовый монослой — рисунок без фона,
 * ровно под перекраску. Ниже тринадцатой и у приложений, которые слой не
 * положили, силуэт считаем сами: тёмное становится рисунком, светлое уходит.
 */
enum class MonoMode {
    /** Обычные цветные значки. */
    Off,

    /** Красим только те, у кого система отдала монослой. */
    Declared,

    /** Красим всё: чего нет, то считаем силуэтом. */
    Always;

    companion object {
        fun of(name: String?): MonoMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Off
    }
}

object MonoIcons {

    /**
     * Монослой приложения.
     *
     * Возвращает null, когда система его не знает: у вызывающего остаётся
     * выбор — рисовать цветной значок или считать силуэт.
     */
    fun layerOf(drawable: Drawable): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        if (drawable !is AdaptiveIconDrawable) return null
        return drawable.monochrome
    }

    /**
     * Силуэт из цветной иконки.
     *
     * Прозрачность берём от того, насколько пиксель темнее фона: у логотипа на
     * белом остаётся рисунок, у светлого на тёмном — он же, только наоборот.
     * Порог считаем от средней яркости самой картинки, иначе плоские значки
     * либо чернеют целиком, либо пропадают.
     */
    fun silhouette(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var sum = 0.0
        var counted = 0
        pixels.forEach { pixel ->
            if (Color.alpha(pixel) < 24) return@forEach
            sum += luma(pixel)
            counted++
        }
        if (counted == 0) return source
        val average = sum / counted
        // Светлая картинка — рисунок это тёмные места, и наоборот.
        val darkOnLight = average > 0.5

        val output = IntArray(pixels.size)
        pixels.forEachIndexed { index, pixel ->
            val alpha = Color.alpha(pixel)
            if (alpha < 24) {
                output[index] = 0
                return@forEachIndexed
            }
            val value = luma(pixel)
            val distance = if (darkOnLight) (average - value) else (value - average)
            // Полтора запаса по контрасту: слабая разница уже даёт заметный след.
            val strength = (distance * 2.6).coerceIn(0.0, 1.0)
            val out = (strength * alpha).toInt().coerceIn(0, 255)
            output[index] = Color.argb(out, 255, 255, 255)
        }

        val result = createBitmap(width, height)
        result.setPixels(output, 0, width, 0, 0, width, height)
        return result
    }

    /** Рисунок одним цветом на своей подложке. */
    fun paint(mask: Bitmap, sizePx: Int, tint: Int, background: Int): Bitmap {
        val output = createBitmap(sizePx, sizePx)
        val canvas = Canvas(output)
        canvas.drawColor(background)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
        }
        val source = android.graphics.Rect(0, 0, mask.width, mask.height)
        val target = android.graphics.Rect(0, 0, sizePx, sizePx)
        canvas.drawBitmap(mask, source, target, paint)
        return output
    }

    private fun luma(pixel: Int): Double {
        val r = Color.red(pixel) / 255.0
        val g = Color.green(pixel) / 255.0
        val b = Color.blue(pixel) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}

/**
 * Как красить значок: режим и пара цветов.
 *
 * Цвета приходят из темы, поэтому входят в ключ кэша — иначе после смены
 * палитры на экране останутся картинки, покрашенные вчерашним цветом.
 */
data class MonoStyle(
    val mode: MonoMode,
    val tint: Int,
    val background: Int,
) {
    val key: String get() = "${mode.name}:${tint.toUInt().toString(16)}:${background.toUInt().toString(16)}"
}
