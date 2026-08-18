package app.plein.screenshot

import app.plein.ui.dream.DreamContent
import app.plein.ui.theme.DisplayFont
import org.junit.Test

/** Заставка «Полночь»: тонкие часы и полоса ночи на чёрном. */
class DreamScreenshotTest : ScreenshotTest() {

    @Test
    fun `лицо заставки`() {
        snap("dream", dark = true) {
            DreamContent(
                time = "02:40",
                date = "вторник, 18 августа",
                left = "4 ч 20 мин",
                progress = 0.53f,
                clockFamily = DisplayFont,
            )
        }
    }

    @Test
    fun `заставка без будильника`() {
        snap("dream-no-alarm", dark = true) {
            DreamContent(
                time = "02:40",
                date = "вторник, 18 августа",
                left = null,
                progress = null,
                clockFamily = DisplayFont,
            )
        }
    }
}
