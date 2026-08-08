package app.plein.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Экран бывает шире телефона.
 *
 * На планшете и раскрытой раскладушке телефонная сетка выглядит растянутой:
 * четыре огромных значка на восьмистах точках ширины. Считаем колонки от
 * ширины, а лист держим в разумных полях, чтобы строка не тянулась через весь
 * экран.
 */
object Adaptive {

    /** Сколько колонок просит экран такой ширины. */
    fun columnsFor(widthDp: Int, chosen: Int): Int = when {
        widthDp >= 900 -> chosen + 4
        widthDp >= 700 -> chosen + 3
        widthDp >= 600 -> chosen + 2
        widthDp >= 480 -> chosen + 1
        else -> chosen
    }.coerceIn(3, 12)

    /** Поля листа: на широком экране контент не должен расползаться. */
    fun sheetPadding(widthDp: Int): Dp = when {
        widthDp >= 900 -> 48.dp
        widthDp >= 600 -> 32.dp
        else -> 0.dp
    }

    /** Широкий экран показывает поиск и списки в две колонки. */
    fun twoColumns(widthDp: Int): Boolean = widthDp >= 700
}
