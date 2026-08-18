package app.plein.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.util.Locale

/**
 * Язык интерфейса так, как его видит композиция.
 *
 * `Locale.getDefault()` внутри composable читается один раз и больше не
 * меняется: после смены языка дата на кадре и в поиске оставалась на прежнем
 * до перезапуска лаунчера. Конфигурация — наблюдаемое состояние, и при смене
 * языка Compose пересобирает всё, что её читало.
 *
 * Любой формат даты и времени в интерфейсе берёт язык отсюда.
 */
@Composable
fun rememberLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    }
}
