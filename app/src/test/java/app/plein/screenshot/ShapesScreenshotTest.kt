package app.plein.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.plein.ui.settings.ShapesPreviewPanel
import org.junit.Test

/**
 * Формы значков.
 *
 * Выбранная фигура крупнее прочих, и на телефоне её срезало сверху и снизу:
 * масштаб выходил за клетку. Снимок держит этот случай на виду.
 */
class ShapesScreenshotTest : ScreenshotTest() {

    @Test
    fun `формы значков с выбранной`() {
        snap("shapes", dark = true) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                ShapesPreviewPanel()
            }
        }
    }
}
