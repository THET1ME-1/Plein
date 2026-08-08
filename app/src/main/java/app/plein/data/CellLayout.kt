package app.plein.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Место элемента в сетке: левый верхний угол и размер в ячейках.
 *
 * Строки не ограничены сверху — страница растёт вниз и прокручивается. Иначе
 * при переходе с шести колонок на три половина плиток оказывалась бы за краем.
 */
data class Cell(
    val row: Int,
    val col: Int,
    val width: Int = 1,
    val height: Int = 1,
) {
    val lastRow: Int get() = row + height - 1
    val lastCol: Int get() = col + width - 1

    fun overlaps(other: Cell): Boolean =
        col <= other.lastCol && other.col <= lastCol &&
            row <= other.lastRow && other.row <= lastRow

    fun withinColumns(columns: Int): Boolean = col >= 0 && lastCol < columns
}

/** Что стоит в ячейке: приложение или плитка лаунчера. */
sealed interface CellItem {
    val id: String

    data class App(val key: String) : CellItem {
        override val id: String get() = "app:$key"
    }

    data class Tile(val kind: String) : CellItem {
        override val id: String get() = "tile:$kind"
    }
}

data class Placement(val item: CellItem, val cell: Cell)

/**
 * Раскладка страницы.
 *
 * Правило простое: плитки стоят там, куда их поставил человек, а приложения
 * заполняют оставшиеся клетки по порядку. Поэтому список приложений может
 * меняться сколько угодно — раскладка от этого не рассыпается.
 */
object CellLayout {

    /** Первая свободная клетка под размер, слева направо и сверху вниз. */
    fun firstFree(taken: List<Cell>, columns: Int, width: Int, height: Int): Cell {
        if (width > columns) return Cell(nextRow(taken), 0, columns, height)
        var row = 0
        while (row < MAX_ROWS) {
            for (col in 0..(columns - width)) {
                val candidate = Cell(row, col, width, height)
                if (taken.none { it.overlaps(candidate) }) return candidate
            }
            row++
        }
        return Cell(nextRow(taken), 0, width, height)
    }

    private fun nextRow(taken: List<Cell>): Int = (taken.maxOfOrNull { it.lastRow } ?: -1) + 1

    /**
     * Собрать страницу: плитки на своих местах, приложения в остатке.
     *
     * Плитка, которая не влезает в текущее число колонок, ужимается по ширине
     * и переезжает к левому краю: экран мог стать уже, а терять её нельзя.
     */
    fun build(
        apps: List<String>,
        tiles: List<Placement>,
        columns: Int,
    ): List<Placement> {
        val placed = mutableListOf<Placement>()
        val taken = mutableListOf<Cell>()

        tiles.forEach { placement ->
            val wanted = placement.cell
            val fitted = when {
                wanted.withinColumns(columns) && taken.none { it.overlaps(wanted) } -> wanted
                else -> {
                    val width = wanted.width.coerceAtMost(columns)
                    firstFree(taken, columns, width, wanted.height)
                }
            }
            placed += Placement(placement.item, fitted)
            taken += fitted
        }

        apps.forEach { key ->
            val cell = firstFree(taken, columns, 1, 1)
            placed += Placement(CellItem.App(key), cell)
            taken += cell
        }
        return placed
    }

    /** Сколько строк занимает страница. */
    fun rowsOf(placements: List<Placement>): Int =
        (placements.maxOfOrNull { it.cell.lastRow } ?: -1) + 1

    /** Свободно ли место под перенос — с оглядкой на саму переносимую плитку. */
    fun canPlace(placements: List<Placement>, moving: CellItem, cell: Cell, columns: Int): Boolean {
        if (!cell.withinColumns(columns) || cell.row < 0) return false
        return placements.none { it.item.id != moving.id && it.cell.overlaps(cell) }
    }

    fun encode(tiles: List<Placement>): String {
        val array = JSONArray()
        tiles.forEach { placement ->
            val item = placement.item
            if (item !is CellItem.Tile) return@forEach
            array.put(
                JSONObject().apply {
                    put("kind", item.kind)
                    put("row", placement.cell.row)
                    put("col", placement.cell.col)
                    put("w", placement.cell.width)
                    put("h", placement.cell.height)
                }
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<Placement> = runCatching {
        val array = JSONArray(raw ?: return emptyList())
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Placement(
                item = CellItem.Tile(item.getString("kind")),
                cell = Cell(
                    row = item.getInt("row"),
                    col = item.getInt("col"),
                    width = item.optInt("w", 1),
                    height = item.optInt("h", 1),
                ),
            )
        }
    }.getOrDefault(emptyList())

    private const val MAX_ROWS = 40
}
