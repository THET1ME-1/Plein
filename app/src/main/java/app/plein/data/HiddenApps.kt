package app.plein.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Спрятанные приложения.
 *
 * Их нет ни в сетке, ни в поиске, ни в выборе приложения для погоды. Открытая
 * дверь живёт недолго: ушли с экрана — замок защёлкивается снова, иначе
 * «спрятано» означало бы «спрятано до первого раза».
 */
class HiddenApps(context: Context) {

    private val sp = context.getSharedPreferences("plein_hidden", Context.MODE_PRIVATE)

    var keys by mutableStateOf(sp.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet())
        private set

    /** Дверь открыта: показываем спрятанное, пока не ушли с экрана. */
    var unlocked by mutableStateOf(false)

    val isEmpty: Boolean get() = keys.isEmpty()

    fun contains(key: String) = key in keys

    fun toggle(key: String) {
        keys = if (key in keys) keys - key else keys + key
        sp.edit().putStringSet(KEY_HIDDEN, keys).apply()
    }

    fun lock() {
        unlocked = false
    }

    /**
     * Список без спрятанного.
     *
     * Когда дверь открыта, отдаём всё: на странице «Скрытое» человек и ждёт
     * увидеть то, что убрал.
     */
    fun visible(apps: List<AppEntry>): List<AppEntry> =
        if (keys.isEmpty()) apps else apps.filterNot { it.key in keys }

    fun hidden(apps: List<AppEntry>): List<AppEntry> =
        if (keys.isEmpty()) emptyList() else apps.filter { it.key in keys }

    private companion object {
        const val KEY_HIDDEN = "hidden"
    }
}
