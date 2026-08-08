package app.plein.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.plein.ui.menu.MenuPreviewRow
import app.plein.ui.menu.MenuPreviewSection
import org.junit.Test

/**
 * Меню долгого нажатия с полным набором.
 *
 * Пять быстрых действий, четыре пункта приложения и три папки — тот случай,
 * на котором лист перестал помещаться в экран и папки уезжали за край.
 */
class MenuScreenshotTest : ScreenshotTest() {

    @Test
    fun `меню с полным набором`() {
        snap("menu-full", dark = true) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                MenuPreviewSection("Быстрые действия")
                repeat(5) { index ->
                    MenuPreviewRow(Icons.Rounded.OpenInNew, "Действие ${index + 1}")
                }
                MenuPreviewSection("Приложение")
                MenuPreviewRow(Icons.Rounded.SwapHoriz, "Переставить", "Перетащите значок")
                MenuPreviewRow(Icons.Rounded.Info, "О приложении", "Права, память, батарея")
                MenuPreviewRow(Icons.Rounded.Delete, "Удалить", "Убирает приложение", danger = true)
                MenuPreviewSection("Папки")
                repeat(3) { index ->
                    MenuPreviewRow(Icons.Rounded.FolderOpen, "Папка ${index + 1}", "Добавить")
                }
            }
        }
    }
}
