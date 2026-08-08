package app.plein.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Заряд и питание. */
data class BatteryState(val percent: Int, val charging: Boolean)

/**
 * Состояние батареи без опроса.
 *
 * Система сама рассылает ACTION_BATTERY_CHANGED, поэтому таймер здесь лишний:
 * подписываемся, пока плитка на экране, и отписываемся, когда ушла.
 */
@Composable
fun rememberBattery(): BatteryState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(read(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                state = parse(intent) ?: return
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return state
}

private fun read(context: Context): BatteryState {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return parse(intent) ?: BatteryState(percent = 0, charging = false)
}

private fun parse(intent: Intent?): BatteryState? {
    if (intent == null) return null
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return BatteryState(
        percent = (level * 100f / scale).toInt(),
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL,
    )
}
