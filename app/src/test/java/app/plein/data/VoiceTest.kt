package app.plein.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Голосовой ввод на телефоне без распознавания.
 *
 * На прошивках без сервисов Google слушать некому, и кнопка микрофона должна
 * пропадать. Прошлая проверка считала годным любой экран, который взялся
 * обслужить интент, — человек попадал в системные настройки помощника.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `без распознавания кнопки нет`() {
        assertFalse(Voice.available(context))
    }

    @Test
    fun `экран настроек за помощника не считается`() {
        assertFalse(Voice.assistantAvailable(context))
    }
}
