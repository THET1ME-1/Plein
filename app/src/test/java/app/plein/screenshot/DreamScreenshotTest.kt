package app.plein.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.plein.ui.dream.DreamContent
import app.plein.ui.theme.DisplayFont
import app.plein.ui.theme.PleinTheme
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

    /** Цвет часов идёт от кадра рабочего стола: лес, закат, фьорд. */
    @Test
    fun `цвет заставки от кадра`() {
        val seeds = listOf(0xFF3E7A52, 0xFFC0863E, 0xFF2E5D73)
        snap("dream-colors", dark = true) {
            Row(Modifier.fillMaxSize()) {
                seeds.forEach { seed ->
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        PleinTheme(dark = true, seed = Color(seed.toInt()), interfaceFont = "") {
                            DreamContent(
                                time = "02:40",
                                date = "вторник",
                                left = "4 ч 20 мин",
                                progress = 0.53f,
                                clockFamily = DisplayFont,
                            )
                        }
                    }
                }
            }
        }
    }
}
