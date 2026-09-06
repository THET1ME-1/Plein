package app.plein.gesture

import android.content.ComponentName
import android.os.Process
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.plein.data.AppEntry
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
 * Удержание значка на странице с плитками.
 *
 * У страницы с плитками свой обработчик касаний, и он забирал долгий тап себе:
 * значок повисал в переносе, а меню не открывалось. Меню — это единственный
 * путь к «переименовать», «спрятать» и «переставить», поэтому жест проверяется
 * тестом.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class AppLongPressTest {

    @get:Rule
    val compose = createComposeRule()

    private val rowHeight = 96.dp

    private fun entry(name: String) = AppEntry(
        label = name,
        component = ComponentName("app.test.$name", "app.test.$name.Main"),
        user = Process.myUserHandle(),
    )

    @Composable
    private fun page(
        apps: List<AppEntry>,
        editing: Boolean,
        onClick: (AppEntry) -> Unit,
        onLongClick: (AppEntry) -> Unit,
        onReorder: (List<String>) -> Unit,
        onStartEditing: () -> Unit,
    ) {
        PleinTheme(dark = true, interfaceFont = "") {
            TilePage(
                apps = apps,
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
                editing = editing,
                tileContent = {},
                widgetContent = { _ -> },
                onClick = onClick,
                onLongClick = onLongClick,
                onTileMenu = {},
                onTileAction = {},
                onTileMove = { _, _ -> true },
                onTileRemove = {},
                onReorder = onReorder,
                onStartEditing = onStartEditing,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun `удержание значка открывает меню`() {
        val first = entry("one")
        var menuFor: AppEntry? = null
        var launched: AppEntry? = null
        var editingStarted = false

        compose.setContent {
            page(
                apps = listOf(first),
                editing = false,
                onClick = { launched = it },
                onLongClick = { menuFor = it },
                onReorder = {},
                onStartEditing = { editingStarted = true },
            )
        }

        compose.onNodeWithTag(CellItem.App(first.key).id).performTouchInput {
            down(center)
            advanceEventTime(700)
            up()
        }
        compose.waitForIdle()

        assertEquals("меню не открылось", first, menuFor)
        assertEquals("приложение запустилось вместо меню", null, launched)
        assertEquals("удержание включило правку", false, editingStarted)
    }

    @Test
    fun `короткое касание запускает приложение`() {
        val first = entry("one")
        var menuFor: AppEntry? = null
        var launched: AppEntry? = null

        compose.setContent {
            page(
                apps = listOf(first),
                editing = false,
                onClick = { launched = it },
                onLongClick = { menuFor = it },
                onReorder = {},
                onStartEditing = {},
            )
        }

        compose.onNodeWithTag(CellItem.App(first.key).id).performTouchInput {
            down(center)
            advanceEventTime(40)
            up()
        }
        compose.waitForIdle()

        assertEquals("приложение не запустилось", first, launched)
        assertEquals("вместо запуска открылось меню", null, menuFor)
    }

    @Test
    fun `в правке значок переезжает на пустую клетку`() {
        val first = entry("one")
        val second = entry("two")
        var order: List<String>? = null

        compose.setContent {
            page(
                apps = listOf(first, second),
                editing = true,
                onClick = {},
                onLongClick = {},
                onReorder = { order = it },
                onStartEditing = {},
            )
        }

        // Плитка 2×2 стоит в левом верхнем углу, значки идут за ней: (0,2) и
        // (0,3). Тащим первый на пустую клетку строкой ниже — он должен встать
        // последним, а не вернуться на место.
        compose.onNodeWithTag(CellItem.App(first.key).id).performTouchInput {
            down(center)
            advanceEventTime(700)
            moveTo(center + Offset(0f, rowHeight.toPx() * 0.6f))
            advanceEventTime(40)
            moveTo(center + Offset(0f, rowHeight.toPx()))
            advanceEventTime(40)
            up()
        }
        compose.waitForIdle()

        assertEquals("порядок не изменился", listOf(second.key, first.key), order)
    }
}
