package app.plein.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createComposeRule
import app.plein.ui.theme.PleinTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Основание для снимков экрана.
 *
 * Всё считается на JVM через Robolectric, эмулятор не нужен. Экран задан
 * жёстко: без квалификатора Robolectric берёт свой крошечный и снимки
 * расходятся от машины к машине.
 *
 * Шрифты Google Fonts в тесте не приезжают, поэтому тема берёт вшитые: иначе
 * первый прогон рисует один шрифт, второй другой.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
abstract class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    protected fun snap(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            PleinTheme(dark = dark, seed = Color(0xFFC0863E), interfaceFont = "") {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
