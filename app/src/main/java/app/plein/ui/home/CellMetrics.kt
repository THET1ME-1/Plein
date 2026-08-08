package app.plein.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Размеры клетки.
 *
 * Высота ряда не может быть просто числом из настроек: размер значка зависит
 * от числа колонок, и на трёх колонках значок 82 dp в клетку 96 dp вместе с
 * подписью не влезал — низ срезало. Поэтому своя высота уважается ровно до
 * той черты, за которой она начинает резать содержимое.
 */
object CellMetrics {

    /** Место под подпись и отступы вокруг значка. */
    const val LABEL_SPACE = 26f

    /** Отступ без подписи: только воздух сверху и снизу. */
    const val BARE_SPACE = 12f

    fun rowHeightFor(columns: Int, showLabels: Boolean): Dp =
        (iconSizeFor(columns).value + if (showLabels) LABEL_SPACE else BARE_SPACE).dp

    /** Своя высота, но не меньше той, в которую влезает значок. */
    fun resolve(custom: Int, columns: Int, showLabels: Boolean): Dp {
        val needed = rowHeightFor(columns, showLabels)
        return if (custom >= needed.value) custom.dp else needed
    }
}
