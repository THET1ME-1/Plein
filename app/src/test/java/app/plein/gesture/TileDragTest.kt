package app.plein.gesture

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.plein.data.Cell
import app.plein.data.CellItem
import app.plein.data.MonoMode
import app.plein.data.Placement
import app.plein.ui.home.TilePage
import app.plein.ui.icons.IconShape
import app.plein.ui.theme.PleinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Перетаскивание плитки.
 *
 * Живой палец на телефоне повторить нечем, а жест ломался уже трижды — значит
 * он обязан проверяться сам. Держим удержание, движение и отпускание: плитка
 * должна доехать до соседней клетки.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class TileDragTest {

    @get:Rule
    val compose = createComposeRule()

    private val rowHeight = 96.dp

    @Test
    fun `плитка переезжает на соседнюю клетку`() {
        var movedTo: Cell? = null
        var editingStarted = false

        compose.setContent {
            PleinTheme(dark = true, interfaceFont = "") {
                TilePage(
                    apps = emptyList(),
                    tiles = listOf(
                        Placement(CellItem.Tile("clock"), Cell(row = 0, col = 0, width = 2, height = 2))
                    ),
                    repository = app.plein.data.AppRepository(
                        androidx.test.core.app.ApplicationProvider.getApplicationContext()
                    ),
                    columns = 4,
                    rowHeight = rowHeight,
                    iconSize = 56.dp,
                    iconShape = IconShape.Default,
                    iconPack = "",
                    monoMode = MonoMode.Off,
                    showLabels = true,
                    editing = false,
                    tileContent = {},
                    widgetContent = { _, _, _ -> },
                    onClick = {},
                    onLongClick = {},
                    onTileMenu = {},
                    onTileAction = {},
                    onTileMove = { _, cell -> movedTo = cell; true },
                    onTileRemove = {},
                    onReorder = {},
                    onStartEditing = { editingStarted = true },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Один жест целиком: время двигаем часами ввода — таймаут долгого
        // нажатия внутри жеста живёт именно на них.
        compose.onNodeWithTag("tile:clock").performTouchInput {
            down(center)
            advanceEventTime(700)
            // Двигаем в пределах самой плитки: за её краем события уходят
            // другому узлу, и тест мерил бы не жест, а границы.
            moveTo(center + Offset(0f, rowHeight.toPx() * 0.5f))
            advanceEventTime(40)
            moveTo(center + Offset(0f, rowHeight.toPx() * 0.9f))
            advanceEventTime(40)
            moveTo(center + Offset(0f, rowHeight.toPx()))
            advanceEventTime(40)
            up()
        }
        compose.waitForIdle()

        assertEquals("правка не включилась удержанием", true, editingStarted)
        assertNotNull("плитка не поехала вовсе", movedTo)
        assertEquals("уехала не туда: $movedTo", 1, movedTo!!.row)
        assertEquals("колонка съехала: $movedTo", 0, movedTo!!.col)
    }
}
