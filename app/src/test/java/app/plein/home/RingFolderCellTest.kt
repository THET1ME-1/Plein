package app.plein.home

import android.content.ComponentName
import android.os.Process
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.plein.data.AppEntry
import app.plein.data.MonoMode
import app.plein.data.RingFolder
import app.plein.ui.home.RingFolderCell
import app.plein.ui.icons.IconShape
import app.plein.ui.theme.PleinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Круговая папка на экране: середина считает остаток, кольцо запускает.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class RingFolderCellTest {

    @get:Rule
    val compose = createComposeRule()

    private fun entry(name: String) = AppEntry(
        label = name,
        component = ComponentName("app.test.$name", "app.test.$name.Main"),
        user = Process.myUserHandle(),
    )

    private fun cell(
        count: Int,
        interactive: Boolean = true,
        onLaunch: (AppEntry) -> Unit = {},
        onOpen: () -> Unit = {},
    ) {
        val entries = (1..count).map { entry("app$it") }
        compose.setContent {
            PleinTheme(dark = true, interfaceFont = "") {
                RingFolderCell(
                    folder = RingFolder("r1", "Дом", entries.map { it.key }),
                    apps = entries.associateBy { it.key },
                    repository = app.plein.data.AppRepository(
                        androidx.test.core.app.ApplicationProvider.getApplicationContext()
                    ),
                    iconShape = IconShape.Default,
                    iconPack = "",
                    monoMode = MonoMode.Off,
                    interactive = interactive,
                    onLaunch = onLaunch,
                    onOpen = onOpen,
                    modifier = Modifier.size(160.dp),
                )
            }
        }
    }

    @Test
    fun `середина показывает, сколько осталось за кольцом`() {
        cell(count = 13)
        compose.onNodeWithText("+7").assertIsDisplayed()
    }

    @Test
    fun `неполная папка не показывает счётчик`() {
        cell(count = 4)
        compose.onNodeWithTag("ring:r1").assertIsDisplayed()
        compose.onAllNodesWithTextSafe("+")
    }

    @Test
    fun `значки стоят по окружности, первый сверху`() {
        cell(count = 6)
        val disc = compose.onNodeWithTag("ring:r1").fetchSemanticsNode().boundsInRoot
        val middle = androidx.compose.ui.geometry.Offset(
            x = (disc.left + disc.right) / 2,
            y = (disc.top + disc.bottom) / 2,
        )
        // Сторона папки: клетка квадратная, снимаем по ширине.
        val side = disc.right - disc.left
        val radius = side * app.plein.data.RingFolders.RADIUS

        val places = (0 until 6).map { index ->
            val box = compose.onNodeWithTag("ring:r1:$index").fetchSemanticsNode().boundsInRoot
            androidx.compose.ui.geometry.Offset(
                x = (box.left + box.right) / 2,
                y = (box.top + box.bottom) / 2,
            )
        }

        places.forEachIndexed { index, place ->
            val away = kotlin.math.hypot(place.x - middle.x, place.y - middle.y)
            assertEquals("значок $index сошёл с окружности", radius, away, side * 0.02f)
        }

        // Первое место строго сверху: над серединой и на её вертикали.
        assertEquals("первый значок съехал вбок", middle.x, places[0].x, side * 0.02f)
        assertEquals("первый значок не сверху", middle.y - radius, places[0].y, side * 0.02f)
        // Четвёртое — ровно снизу: значит обход идёт по часовой стрелке.
        assertEquals("четвёртый значок не снизу", middle.y + radius, places[3].y, side * 0.02f)
    }

    @Test
    fun `в правке кольцо касаний не ловит`() {
        var opened = false
        cell(count = 8, interactive = false, onOpen = { opened = true })
        compose.onNodeWithTag("ring:r1").performClick()
        compose.waitForIdle()
        assertEquals("в правке касание не должно раскрывать папку", false, opened)
    }
}

/** Счётчика в неполной папке быть не должно: проверяем отсутствием узла. */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextSafe(text: String) {
    val nodes = onAllNodes(
        androidx.compose.ui.test.hasText(text, substring = true)
    ).fetchSemanticsNodes()
    assertNull("счётчик показан, хотя остатка нет", nodes.firstOrNull())
}
