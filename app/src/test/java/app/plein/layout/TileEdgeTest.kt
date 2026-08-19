package app.plein.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.plein.data.Cell
import app.plein.data.CellItem
import app.plein.data.MonoMode
import app.plein.data.Placement
import app.plein.ui.home.TilePage
import app.plein.ui.icons.IconShape
import app.plein.ui.theme.PleinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Края плиток.
 *
 * Плитка во всю строку выходит к полям страницы, как заголовок папки: пока
 * она отступала внутрь на воздух иконки, справа оставалась щель и рядом со
 * строкой значков плитка выглядела уже неё. Плитка посреди строки, наоборот,
 * держит этот воздух — иначе соседи склеиваются.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class TileEdgeTest {

    @get:Rule
    val compose = createComposeRule()

    private val columns = 4
    private val iconSize = 61.dp
    private val page = 411f
    private val side = 20f
    private val cell = (page - side * 2) / columns
    private val inset = (cell - iconSize.value) / 2

    private fun show(cell: Cell) {
        compose.setContent {
            PleinTheme(dark = true, interfaceFont = "") {
                TilePage(
                    apps = emptyList(),
                    tiles = listOf(Placement(CellItem.Tile("media"), cell)),
                    repository = app.plein.data.AppRepository(
                        androidx.test.core.app.ApplicationProvider.getApplicationContext()
                    ),
                    columns = columns,
                    rowHeight = 84.dp,
                    iconSize = iconSize,
                    iconShape = IconShape.Default,
                    iconPack = "",
                    monoMode = MonoMode.Off,
                    showLabels = false,
                    editing = false,
                    tileContent = { Box(Modifier.fillMaxSize().testTag("body")) },
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

    @Test
    fun `плитка во всю строку кончается там же, где ряд значков`() {
        show(Cell(row = 0, col = 0, width = columns, height = 2))

        val bounds = compose.onNodeWithTag("body").getUnclippedBoundsInRoot()
        assertEquals(side + inset, bounds.left.value, 1f)
        assertEquals(page - side - inset, bounds.right.value, 1f)
    }

    @Test
    fun `у плитки в полстроки воздух с обеих сторон одинаковый`() {
        show(Cell(row = 0, col = 0, width = 2, height = 2))

        val bounds = compose.onNodeWithTag("body").getUnclippedBoundsInRoot()
        assertEquals(side + inset, bounds.left.value, 1f)
        assertEquals(side + cell * 2 - inset, bounds.right.value, 1f)
    }

    @Test
    fun `плитка посреди строки держит воздух с обеих сторон`() {
        show(Cell(row = 0, col = 1, width = 2, height = 1))

        val bounds = compose.onNodeWithTag("body").getUnclippedBoundsInRoot()
        assertEquals(side + cell + inset, bounds.left.value, 1f)
        assertEquals(side + cell * 3 - inset, bounds.right.value, 1f)
    }
}
