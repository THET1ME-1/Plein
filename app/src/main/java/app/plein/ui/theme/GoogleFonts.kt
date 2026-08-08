package app.plein.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import app.plein.R

/**
 * Шрифты из Google Fonts.
 *
 * Ничего не скачиваем сами: семейство отдаёт провайдер Play Services по имени,
 * система кэширует его между приложениями.
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

@Composable
fun googleFontFamily(name: String): FontFamily = remember(name) {
    FontFamily(Font(googleFont = GoogleFont(name), fontProvider = provider))
}
