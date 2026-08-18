package app.plein.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.plein.R

/**
 * Типографика ДНК: Unbounded в заголовках, Onest в тексте.
 * Веса и трекинг взяты из m3-dna/GUIDE.md §2 и не меняются.
 */
val DisplayFont = FontFamily(
    // Тонкий вес нужен одной заставке: крупные цифры на чёрном ночью. В
    // остальном интерфейсе веса прежние, ДНК не тронута.
    Font(R.font.unbounded_extralight, FontWeight.W200),
    Font(R.font.unbounded_semibold, FontWeight.W600),
    Font(R.font.unbounded_bold, FontWeight.W700),
    Font(R.font.unbounded_extrabold, FontWeight.W800),
)

val BodyFont = FontFamily(
    Font(R.font.onest_regular, FontWeight.W400),
    Font(R.font.onest_medium, FontWeight.W500),
    Font(R.font.onest_bold, FontWeight.W700),
)

/** Моноширинный для часов, счётчиков и служебных подписей. */
val MonoFont = FontFamily(Font(R.font.jbmono_regular, FontWeight.W400))

private fun display(size: Int, line: Int) = TextStyle(
    fontFamily = DisplayFont, fontWeight = FontWeight.W800,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = (-0.5).sp,
)

private fun headline(size: Int, line: Int) = TextStyle(
    fontFamily = DisplayFont, fontWeight = FontWeight.W700,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = (-0.3).sp,
)

private fun title(size: Int, line: Int) = TextStyle(
    fontFamily = DisplayFont, fontWeight = FontWeight.W600,
    fontSize = size.sp, lineHeight = line.sp,
)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.W400) = TextStyle(
    fontFamily = BodyFont, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
)

val PleinTypography = Typography(
    displayLarge = display(57, 60),
    displayMedium = display(45, 50),
    displaySmall = display(36, 40),
    headlineLarge = headline(32, 36),
    headlineMedium = headline(28, 32),
    headlineSmall = headline(24, 28),
    titleLarge = title(22, 26),
    titleMedium = title(16, 22),
    titleSmall = title(14, 20),
    bodyLarge = body(16, 24),
    bodyMedium = body(14, 20),
    bodySmall = body(12, 16),
    labelLarge = body(14, 20, FontWeight.W700),
    labelMedium = body(12, 16, FontWeight.W500),
    labelSmall = body(11, 14, FontWeight.W500),
)
