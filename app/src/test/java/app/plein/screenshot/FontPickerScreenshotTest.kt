package app.plein.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.plein.ui.settings.FontRow
import app.plein.ui.settings.PlainField
import app.plein.ui.settings.RowPlace
import app.plein.ui.settings.SettingsPanel
import org.junit.Test

/**
 * Список шрифтов и поле темы кадра.
 *
 * Провайдер Google Fonts на JVM не отвечает, поэтому снимок показывает ровно
 * то, что увидит человек на телефоне без сервисов Google: строки с честной
 * пометкой вместо девяноста девяти одинаковых названий.
 */
class FontPickerScreenshotTest : ScreenshotTest() {

    @Test
    fun `строки шрифтов с образцом`() {
        snap("font-rows") {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                FontRow(
                    family = "",
                    title = "Unbounded · Onest",
                    selected = true,
                    available = true,
                    onClick = {},
                )
                // Провайдер есть, но шрифт не приехал: так выглядит отказ.
                FontRow("Alegreya", "Alegreya", selected = false, available = true, onClick = {})
                FontRow("Bebas Neue", "Bebas Neue", selected = false, available = true, onClick = {})
                // Провайдера нет вовсе: строку не мучаем, наверху листа плашка.
                FontRow("Anton", "Anton", selected = false, available = false, onClick = {})
                FontRow("Archivo", "Archivo", selected = false, available = false, onClick = {})
            }
        }
    }

    @Test
    fun `поле темы кадра`() {
        snap("backdrop-theme", dark = true) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                SettingsPanel(title = "Тема кадра", place = RowPlace.Single) {
                    PlainField(
                        value = "море, снег",
                        onValueChange = {},
                        placeholder = "море, снег, горы",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Слова уходят в фотобанк как есть. Пусто — лаунчер подбирает сам. " +
                            "Свои снимки и папка тему не смотрят.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
