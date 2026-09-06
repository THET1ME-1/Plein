package app.plein.screenshot

import android.content.ComponentName
import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.data.AppEntry
import app.plein.data.Cell
import app.plein.data.CellItem
import app.plein.data.CellLayout
import app.plein.data.MonoMode
import app.plein.data.Placement
import app.plein.data.RingFolder
import app.plein.ui.home.CellGrid
import app.plein.ui.home.ClockTile
import app.plein.ui.home.RingFolderCell
import app.plein.ui.icons.IconShape
import org.junit.Test

/**
 * Круговая папка рядом с плиткой часов — та же раскладка, что на телефоне.
 *
 * Снимок нужен, потому что кольцо считается арифметикой: числа проверяет
 * `RingFolderTest`, а как это выглядит вместе с соседями — видно только здесь.
 */
class RingFolderScreenshotTest : ScreenshotTest() {

    private fun entry(name: String) = AppEntry(
        label = name,
        component = ComponentName("app.test.$name", "app.test.$name.Main"),
        user = Process.myUserHandle(),
    )

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
            Text("$index", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    @Test
    fun `папка на сетке`() {
        val entries = (1..13).map { entry("app$it") }
        val folder = RingFolder("r1", "Дом", entries.map { it.key })
        val tiles = listOf(
            Placement(CellItem.Tile("clock"), Cell(row = 0, col = 0, width = 2, height = 2)),
            Placement(CellItem.Ring("r1"), Cell(row = 0, col = 2, width = 2, height = 2)),
        )
        val placed = CellLayout.build(apps = (1..8).map { "icon$it" }, tiles = tiles, columns = 4)

        snap("ring-folder", dark = true) {
            CellGrid(
                placements = placed,
                columns = 4,
                rowHeight = 92.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { placement ->
                when (val item = placement.item) {
                    is CellItem.App -> FakeIcon(item.key.removePrefix("icon").toInt())
                    is CellItem.Tile -> ClockTile(time = "9:41", date = "СБ, 6 СЕНТ")
                    is CellItem.Ring -> RingFolderCell(
                        folder = folder,
                        apps = entries.associateBy { it.key },
                        repository = app.plein.data.AppRepository(
                            androidx.test.core.app.ApplicationProvider.getApplicationContext()
                        ),
                        iconShape = IconShape.Default,
                        iconPack = "",
                        monoMode = MonoMode.Off,
                        onLaunch = {},
                        onOpen = {},
                    )

                    is CellItem.Widget -> Unit
                }
            }
        }
    }
}
