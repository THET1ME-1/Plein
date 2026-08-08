package app.plein.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.plein.R
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/** Режимы темы ДНК. AutoTime решается выше, сюда приходит уже светлая или тёмная. */
enum class ThemeMode(val titleRes: Int) {
    Light(R.string.theme_light),
    Dark(R.string.theme_dark),
    System(R.string.theme_system),
    AutoTime(R.string.theme_auto_time),
}

/** Насыщенность из ДНК: «Сочно» и «Точь-в-точь». Стоковый tonalSpot не используем. */
enum class Vibrancy(val titleRes: Int) {
    Vibrant(R.string.vibrancy_vibrant),
    Fidelity(R.string.vibrancy_fidelity),
}

/** Тёмная тема по режиму: AutoTime включает её с 20:00 до 07:00. */
fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
    ThemeMode.System -> systemDark
    ThemeMode.AutoTime -> {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        hour >= 20 || hour < 7
    }
}

val DefaultSeed = Color(0xFF2E5D73)

@Composable
fun PleinTheme(
    dark: Boolean,
    seed: Color = DefaultSeed,
    dynamicColor: Boolean = false,
    amoled: Boolean = false,
    vibrancy: Vibrancy = Vibrancy.Vibrant,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> rememberDynamicColorScheme(
            seedColor = seed,
            isDark = dark,
            isAmoled = amoled,
            style = if (vibrancy == Vibrancy.Vibrant) PaletteStyle.Vibrant else PaletteStyle.Fidelity,
        )
    }

    val scheme = remember(base, amoled, dark) {
        if (amoled && dark) base.copy(background = Color.Black, surface = Color.Black) else base
    }

    // Смена seed не должна дёргать глазами: перекрашиваем плавно.
    val surface by animateColorAsState(scheme.surface, label = "surface")
    val primary by animateColorAsState(scheme.primary, label = "primary")
    val animated = scheme.copy(surface = surface, primary = primary)

    MaterialTheme(
        colorScheme = animated,
        typography = PleinTypography,
        shapes = PleinShapes,
        content = content,
    )
}
