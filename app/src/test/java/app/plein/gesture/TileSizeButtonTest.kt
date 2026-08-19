package app.plein.gesture

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
 * Кнопка размера в режиме правки.
 *
 * Размеры и раньше лежали в меню плитки, но открывались вторым касанием уже
 * внутри правки: человек с 4×2 не находил 2×2 вовсе. Теперь у плитки в правке
 * своя кнопка рядом с крестиком.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class TileSizeButtonTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `кнопка размера открывает меню плитки`() {
        var opened: CellItem? = null

        compose.setContent {
            PleinTheme(dark = true, interfaceFont = "") {
                TilePage(
                    apps = emptyList(),
                    tiles = listOf(
                        Placement(CellItem.Tile("media"), Cell(row = 0, col = 0, width = 4, height = 2))
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
                    editing = true,
                    tileContent = {},
                    widgetContent = { _ -> },
                    onClick = {},
                    onLongClick = {},
                    onTileMenu = { opened = it },
                    onTileAction = {},
                    onTileMove = { _, _ -> true },
                    onTileRemove = {},
                    onReorder = {},
                    onStartEditing = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        compose.onNodeWithTag("size:tile:media").performClick()

        assertEquals(CellItem.Tile("media"), opened)
    }
}
