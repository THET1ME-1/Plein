package app.plein.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.ui.home.IndicatorPreviewRow
import org.junit.Test

/** Семь видов индикатора страниц рядом: три из них во всю ширину. */
class IndicatorScreenshotTest : ScreenshotTest() {

    @Test
    fun `виды индикатора`() {
        val styles = listOf(
            "dots" to "Точки",
            "bar" to "Полоса",
            "wide" to "Широкая полоса",
            "edge" to "Линия по краю",
            "segments" to "Сегменты",
            "numbers" to "Числа",
        )
        snap("indicators", dark = true) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                styles.forEach { (style, title) ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 10.dp),
                    )
                    IndicatorPreviewRow(style = style)
                }
            }
        }
    }
}
