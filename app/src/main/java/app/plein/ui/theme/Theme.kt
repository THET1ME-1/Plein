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
    Soft(R.string.vibrancy_soft),
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

/**
 * Каким цветом красить экран.
 *
 * Цвет кадра и ручной цвет живут порознь: человек выключает «брать из кадра»,
 * подбирает свой, потом возвращает — и цвет кадра не пропал, а ручной не
 * затёрся. Ноль означает, что кадра ещё не было ни разу.
 */
object SeedChoice {
    fun of(fromPhoto: Boolean, photo: Int, manual: Int): Int =
        if (fromPhoto && photo != 0) photo else manual
}

@Composable
fun PleinTheme(
    dark: Boolean,
    seed: Color = DefaultSeed,
    dynamicColor: Boolean = false,
    amoled: Boolean = false,
    vibrancy: Vibrancy = Vibrancy.Vibrant,
    interfaceFont: String = "",
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
            style = when (vibrancy) {
                Vibrancy.Soft -> PaletteStyle.TonalSpot
                Vibrancy.Vibrant -> PaletteStyle.Vibrant
                Vibrancy.Fidelity -> PaletteStyle.Fidelity
            },
        )
    }

    val scheme = remember(base, amoled, dark) {
        if (amoled && dark) base.copy(background = Color.Black, surface = Color.Black) else base
    }

    // Смена seed не должна дёргать глазами: перекрашиваем плавно.
    val surface by animateColorAsState(scheme.surface, label = "surface")
    val primary by animateColorAsState(scheme.primary, label = "primary")
    val animated = scheme.copy(surface = surface, primary = primary)

    // Свой шрифт из каталога подменяет и заголовки, и текст.
    val typography = if (interfaceFont.isEmpty()) {
        PleinTypography
    } else {
        val family = googleFontFamily(interfaceFont)
        PleinTypography.copy(
            displayLarge = PleinTypography.displayLarge.copy(fontFamily = family),
            displayMedium = PleinTypography.displayMedium.copy(fontFamily = family),
            displaySmall = PleinTypography.displaySmall.copy(fontFamily = family),
            headlineLarge = PleinTypography.headlineLarge.copy(fontFamily = family),
            headlineMedium = PleinTypography.headlineMedium.copy(fontFamily = family),
            headlineSmall = PleinTypography.headlineSmall.copy(fontFamily = family),
            titleLarge = PleinTypography.titleLarge.copy(fontFamily = family),
            titleMedium = PleinTypography.titleMedium.copy(fontFamily = family),
            titleSmall = PleinTypography.titleSmall.copy(fontFamily = family),
            bodyLarge = PleinTypography.bodyLarge.copy(fontFamily = family),
            bodyMedium = PleinTypography.bodyMedium.copy(fontFamily = family),
            bodySmall = PleinTypography.bodySmall.copy(fontFamily = family),
            labelLarge = PleinTypography.labelLarge.copy(fontFamily = family),
            labelMedium = PleinTypography.labelMedium.copy(fontFamily = family),
            labelSmall = PleinTypography.labelSmall.copy(fontFamily = family),
        )
    }

    MaterialTheme(
        colorScheme = animated,
        typography = typography,
        shapes = PleinShapes,
        content = content,
    )
}
