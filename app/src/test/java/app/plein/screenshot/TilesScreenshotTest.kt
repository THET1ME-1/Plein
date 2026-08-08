package app.plein.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.plein.data.Cell
import app.plein.data.CellItem
import app.plein.data.CellLayout
import app.plein.data.Placement
import app.plein.ui.home.BatteryTile
import app.plein.ui.home.CalendarTile
import app.plein.ui.home.CellGrid
import app.plein.ui.home.ClockTile
import app.plein.ui.home.NoteTile
import app.plein.ui.home.TileSurface
import app.plein.ui.home.Tiles
import app.plein.ui.home.WeatherTile
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Страница с плитками и иконками вперемешку.
 *
 * Ровно та раскладка, которую просил заказчик: ряд приложений, потом плитка в
 * две клетки с иконками сбоку, потом снова ряд.
 */
class TilesScreenshotTest : ScreenshotTest() {

    @Composable
    private fun FakeIcon(index: Int) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$index",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    @Test
    fun `страница с плитками`() {
        val tiles = listOf(
            Placement(CellItem.Tile(Tiles.CLOCK), Cell(row = 1, col = 0, width = 2, height = 2)),
            Placement(CellItem.Tile(Tiles.BATTERY), Cell(row = 3, col = 0, width = 2, height = 1)),
            Placement(CellItem.Tile(Tiles.CALENDAR), Cell(row = 4, col = 0, width = 4, height = 1)),
        )
        val placed = CellLayout.build(List(14) { "app$it" }, tiles, columns = 4)
        assertEquals(4, placed.count { it.cell.row == 0 })

        snap("tiles-page", dark = true) {
            CellGrid(
                placements = placed,
                columns = 4,
                rowHeight = 92.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { placement ->
                when (val item = placement.item) {
                    is CellItem.Widget -> TileSurface {}
                    is CellItem.App -> FakeIcon(item.key.removePrefix("app").toInt())
                    is CellItem.Tile -> when (item.kind) {
                        Tiles.CLOCK -> ClockTile(time = "20:41", date = "СБ, 8 АВГ")
                        Tiles.BATTERY -> BatteryTile(percent = 64, charging = true)
                        Tiles.CALENDAR -> CalendarTile(title = "Созвон с командой", time = "21:30")
                        Tiles.WEATHER -> WeatherTile(temperature = "28°", code = 0, place = "Кишинёв")
                        else -> TileSurface {}
                    }
                }
            }
        }
    }
}
