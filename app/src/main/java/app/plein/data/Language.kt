package app.plein.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/**
 * Язык интерфейса.
 *
 * На Android 13 и новее язык приложения хранит система, там же он виден
 * в настройках телефона. Ниже подменяем список локалей вручную.
 */
object Language {

    val supported = listOf("", "en", "ru", "uk", "de", "fr", "es", "it", "pt")

    fun titleOf(code: String): String = when (code) {
        "" -> "System"
        "en" -> "English"
        "ru" -> "Русский"
        "uk" -> "Українська"
        "de" -> "Deutsch"
        "fr" -> "Français"
        "es" -> "Español"
        "it" -> "Italiano"
        "pt" -> "Português"
        else -> code
    }

    fun apply(context: Context, code: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            manager.applicationLocales =
                if (code.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(code)
        }
    }
}
