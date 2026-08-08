package app.plein.screenshot

import app.plein.ui.dream.DreamContent
import app.plein.ui.theme.MonoFont
import org.junit.Test

/** Заставка: часы шрифтом лаунчера на затемнённом кадре. */
class DreamScreenshotTest : ScreenshotTest() {

    @Test
    fun `лицо заставки`() {
        snap("dream", dark = true) {
            DreamContent(
                time = "23:41",
                date = "суббота, 8 августа",
                photo = null,
                clockFamily = MonoFont,
            )
        }
    }
}
