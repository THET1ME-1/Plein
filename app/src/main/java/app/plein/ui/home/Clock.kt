package app.plein.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Date

/**
 * Текущее время, которое само обновляется.
 *
 * Считать `Date()` прямо в композиции нельзя: значение застывает на моменте
 * сборки экрана и часы стоят, пока лаунчер не пересоздадут. Минуту отбивает
 * система — `ACTION_TIME_TICK` приходит ровно на её начале и только при
 * включённом экране, поэтому будильников и спящих корутин не нужно.
 * `ACTION_SCREEN_ON` закрывает ночь: пока экран был погашен, тиков не было.
 *
 * @param clock источник времени, подменяется в тестах.
 */
@Composable
fun rememberNow(clock: () -> Date = { Date() }): Date {
    val context = LocalContext.current
    var now by remember { mutableStateOf(clock()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                now = clock()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        // Пока экрана не было, минуты шли: берём время сразу после подписки.
        now = clock()

        onDispose { context.unregisterReceiver(receiver) }
    }

    return now
}
