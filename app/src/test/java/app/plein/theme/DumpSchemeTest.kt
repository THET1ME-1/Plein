package app.plein.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import app.plein.ui.theme.DefaultSeed
import app.plein.ui.theme.PleinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Служебный вывод палитры: макет обязан стоять на цветах темы, а не на глаз. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DumpSchemeTest {

    @get:Rule
    val compose = createComposeRule()

    private fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

    private fun dump(name: String, s: ColorScheme) {
        listOf(
            "primary" to s.primary, "onPrimary" to s.onPrimary,
            "primaryContainer" to s.primaryContainer, "onPrimaryContainer" to s.onPrimaryContainer,
            "secondaryContainer" to s.secondaryContainer, "onSecondaryContainer" to s.onSecondaryContainer,
            "tertiaryContainer" to s.tertiaryContainer,
            "background" to s.background, "surface" to s.surface, "onSurface" to s.onSurface,
            "onSurfaceVariant" to s.onSurfaceVariant, "outline" to s.outline,
            "surfaceContainerLowest" to s.surfaceContainerLowest,
            "surfaceContainerLow" to s.surfaceContainerLow,
            "surfaceContainer" to s.surfaceContainer,
            "surfaceContainerHigh" to s.surfaceContainerHigh,
            "surfaceContainerHighest" to s.surfaceContainerHighest,
        ).forEach { (k, v) -> println("SCHEME $name $k ${hex(v)}") }
    }

    @Test
    fun `печать палитры`() {
        var dark: ColorScheme? = null
        var light: ColorScheme? = null
        compose.setContent {
            PleinTheme(dark = true, seed = DefaultSeed) {
                dark = androidx.compose.material3.MaterialTheme.colorScheme
            }
            PleinTheme(dark = false, seed = DefaultSeed) {
                light = androidx.compose.material3.MaterialTheme.colorScheme
            }
        }
        compose.waitForIdle()
        dump("dark", dark!!)
        dump("light", light!!)
    }
}
