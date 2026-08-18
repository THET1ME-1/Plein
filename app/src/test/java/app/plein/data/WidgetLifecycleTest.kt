package app.plein.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Виджет, разрешённый в системном диалоге, обязан пережить убийство процесса.
 *
 * Номер выдаётся до диалога. Пока диалог открыт, лаунчер сидит в фоне, и
 * система его нередко убивает: состояние экрана теряется целиком, результат
 * приходить некуда. Виджет тогда не появлялся, а номер оставался за
 * приложением навсегда — так их и копился десяток за пару месяцев.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetLifecycleTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `ожидание переживает пересоздание`() {
        Widgets(context).rememberPending("all", 42, 2, 2)

        // Новый объект — как после перезапуска процесса.
        val waiting = Widgets(context).pending()

        assertEquals("all", waiting?.folderId)
        assertEquals(42, waiting?.widgetId)
        assertEquals(2, waiting?.width)
        assertEquals(2, waiting?.height)
    }

    @Test
    fun `разобранное ожидание больше не всплывает`() {
        val widgets = Widgets(context)
        widgets.rememberPending("all", 7, 4, 2)
        widgets.clearPending()

        assertNull(Widgets(context).pending())
    }

    @Test
    fun `раскладка отдаёт номера виджетов со всех страниц`() {
        val store = LayoutStore(context)
        store.addWidget("all", 11, 2, 2, columns = 4)
        store.addWidget("games", 12, 2, 2, columns = 4)
        store.add("all", "clock", columns = 4)

        assertEquals(setOf(11, 12), store.widgetIds())
    }
}
