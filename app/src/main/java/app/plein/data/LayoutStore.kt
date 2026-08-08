package app.plein.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf

/**
 * Раскладка плиток по папкам.
 *
 * Приложения здесь не хранятся нарочно: они приходят и уходят, а плитка стоит
 * на месте. Держим только её, а иконки раскладываются вокруг сами.
 */
class LayoutStore(context: Context) {

    private val sp = context.getSharedPreferences("plein_layout", Context.MODE_PRIVATE)
    private val cache = mutableStateMapOf<String, List<Placement>>()

    fun tiles(folderId: String): List<Placement> =
        cache.getOrPut(folderId) { CellLayout.decode(sp.getString(folderId, null)) }

    fun add(folderId: String, kind: String, columns: Int) {
        val current = tiles(folderId)
        val (width, height) = TileSizes.of(kind)
        val cell = CellLayout.firstFree(
            taken = current.map { it.cell },
            columns = columns,
            width = width.coerceAtMost(columns),
            height = height,
        )
        save(folderId, current + Placement(CellItem.Tile(kind), cell))
    }

    /** Виджет приложения: размер приходит от самого приложения. */
    fun addWidget(folderId: String, widgetId: Int, width: Int, height: Int, columns: Int) {
        val current = tiles(folderId)
        val cell = CellLayout.firstFree(
            taken = current.map { it.cell },
            columns = columns,
            width = width.coerceAtMost(columns),
            height = height,
        )
        save(folderId, current + Placement(CellItem.Widget(widgetId), cell))
    }

    fun remove(folderId: String, item: CellItem) {
        save(folderId, tiles(folderId).filterNot { it.item.id == item.id })
    }

    /** Перенос: молча отказываем, если клетка занята или вылезает за край. */
    fun move(folderId: String, item: CellItem, cell: Cell, columns: Int): Boolean {
        val current = tiles(folderId)
        if (!CellLayout.canPlace(current, item, cell, columns)) return false
        save(folderId, current.map { if (it.item.id == item.id) it.copy(cell = cell) else it })
        return true
    }

    fun resize(folderId: String, item: CellItem, width: Int, height: Int, columns: Int): Boolean {
        val current = tiles(folderId)
        val placement = current.firstOrNull { it.item.id == item.id } ?: return false
        val wanted = placement.cell.copy(width = width, height = height)
        if (!CellLayout.canPlace(current, item, wanted, columns)) return false
        save(folderId, current.map { if (it.item.id == item.id) it.copy(cell = wanted) else it })
        return true
    }

    fun has(folderId: String): Boolean = tiles(folderId).isNotEmpty()

    private fun save(folderId: String, tiles: List<Placement>) {
        cache[folderId] = tiles
        sp.edit().putString(folderId, CellLayout.encode(tiles)).apply()
    }
}

/** Размеры плиток в клетках. Держим рядом с данными, а не в отрисовке. */
object TileSizes {

    fun of(kind: String): Pair<Int, Int> = when (kind) {
        "clock" -> 2 to 2
        "weather" -> 2 to 2
        "battery" -> 2 to 1
        "calendar" -> 4 to 1
        "note" -> 4 to 2
        else -> 2 to 2
    }

    /** Во что можно превратить плитку при изменении размера. */
    fun variants(kind: String): List<Pair<Int, Int>> = when (kind) {
        "battery" -> listOf(2 to 1, 2 to 2, 4 to 1)
        "calendar" -> listOf(4 to 1, 4 to 2, 2 to 2)
        "note" -> listOf(4 to 2, 4 to 1, 2 to 2)
        else -> listOf(2 to 2, 4 to 2, 2 to 1, 4 to 1)
    }
}
