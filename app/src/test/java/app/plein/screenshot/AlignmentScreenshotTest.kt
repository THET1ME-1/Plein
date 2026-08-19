package app.plein.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.plein.data.Cell
import app.plein.data.CellItem
import app.plein.data.MonoMode
import app.plein.data.Placement
import app.plein.ui.home.BatteryTile
import app.plein.ui.home.ClockTile
import app.plein.ui.home.TilePage
import app.plein.ui.icons.IconShape
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Плитки вровень со значками.
 *
 * Плитка занимала клетку целиком, а иконка внутри своей клетки уже и прижата
 * к верху — из-за этого нижние и боковые края расходились. Снимок держит их
 * на одной линии.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class AlignmentScreenshotTest : ScreenshotTest() {

    @Test
    fun `плитка вровень со значками`() {
        snap("alignment", dark = true) {
            TilePage(
                apps = emptyList(),
                tiles = listOf(
                    Placement(CellItem.Tile("battery"), Cell(row = 0, col = 1, width = 2, height = 1)),
                    Placement(CellItem.Tile("clock"), Cell(row = 1, col = 0, width = 2, height = 2)),
                ),
                repository = app.plein.data.AppRepository(
                    androidx.test.core.app.ApplicationProvider.getApplicationContext()
                ),
                columns = 4,
                rowHeight = 84.dp,
                iconSize = 61.dp,
                iconShape = IconShape.Default,
                iconPack = "",
                monoMode = MonoMode.Off,
                showLabels = false,
                editing = false,
                tileContent = { kind ->
                    if (kind == "clock") ClockTile(time = "22:33", date = "СБ, 8 АВГ")
                    else BatteryTile(percent = 81, charging = false)
                },
                widgetContent = { _ -> },
                onClick = {},
                onLongClick = {},
                onTileMenu = {},
                onTileAction = {},
                onTileMove = { _, _ -> true },
                onTileRemove = {},
                onReorder = {},
                onStartEditing = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
