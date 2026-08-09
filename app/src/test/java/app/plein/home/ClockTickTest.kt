package app.plein.home

import android.content.Intent
import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.plein.ui.home.rememberNow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Часы на домашнем экране обязаны идти.
 *
 * Раньше время считалось один раз при сборке композиции и застывало: на
 * телефоне держались 2:32, пока лаунчер не пересоздавали.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClockTickTest {

    @get:Rule
    val compose = createComposeRule()

    private val shown = SimpleDateFormat("HH:mm", Locale.ROOT)

    private fun at(time: String): Date =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).parse("2026-08-09 $time")!!

    private fun show(clock: () -> Date) {
        compose.setContent {
            Text(shown.format(rememberNow(clock)), Modifier.testTag("clock"))
        }
    }

    private fun broadcast(action: String) {
        RuntimeEnvironment.getApplication().sendBroadcast(Intent(action))
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `наступила новая минута`() {
        var now = at("02:32")
        show { now }
        compose.onNodeWithTag("clock").assertTextEquals("02:32")

        now = at("02:33")
        broadcast(Intent.ACTION_TIME_TICK)

        compose.onNodeWithTag("clock").assertTextEquals("02:33")
    }

    @Test
    fun `экран проснулся после ночи`() {
        var now = at("02:32")
        show { now }

        now = at("09:14")
        broadcast(Intent.ACTION_SCREEN_ON)

        compose.onNodeWithTag("clock").assertTextEquals("09:14")
    }

    @Test
    fun `часовой пояс сменился в полёте`() {
        var now = at("02:32")
        show { now }

        now = at("04:32")
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        compose.onNodeWithTag("clock").assertTextEquals("04:32")
    }
}
