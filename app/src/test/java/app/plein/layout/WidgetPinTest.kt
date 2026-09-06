package app.plein.layout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.plein.data.Widgets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Закрепление виджета: манифест и ожидание.
 *
 * Приёмник `CONFIRM_PIN_APPWIDGET` — единственное, по чему система решает,
 * умеет ли лаунчер ставить виджеты по просьбе самого приложения. Пока его не
 * было, чужие приложения отвечали, что виджеты не поддерживаются, и никакой
 * код внутри лаунчера этого не исправлял. Проверка стоит здесь, чтобы правка
 * манифеста не убрала его молча.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetPinTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `лаунчер объявлен приёмником закрепления виджета`() {
        val intent = android.content.Intent("android.content.pm.action.CONFIRM_PIN_APPWIDGET")
            .setPackage(context.packageName)
        val found = context.packageManager.queryIntentActivities(intent, 0)
        assertTrue(
            "без приёмника система считает, что лаунчер виджеты не поддерживает",
            found.isNotEmpty(),
        )
        assertEquals(
            "app.plein.PinWidgetActivity",
            found.first().activityInfo.name,
        )
    }

    @Test
    fun `ожидание переживает смерть лаунчера`() {
        val widgets = Widgets(context)
        widgets.rememberPending("folder-1", 42, 2, 2)

        // Новый экземпляр — как после того, как систему убила активность,
        // пока был открыт чужой экран настройки.
        val waiting = Widgets(context).pending()
        assertEquals("folder-1", waiting?.folderId)
        assertEquals(42, waiting?.widgetId)
        assertEquals(2, waiting?.width)
        assertEquals(2, waiting?.height)

        widgets.clearPending()
        assertNull(Widgets(context).pending())
    }
}
