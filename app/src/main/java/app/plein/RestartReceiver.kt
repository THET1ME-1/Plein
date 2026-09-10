package app.plein

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.plein.data.NavGestures

/**
 * Возврат домой после обновления.
 *
 * Android убивает процесс приложения, которое обновляют, и телефон остаётся на
 * системном лаунчере — со стороны это выглядит как вылет. Своё обновление
 * система объявляет отдельно, и по этому объявлению лаунчер поднимается сам.
 *
 * Здесь же возвращается флаг полноэкранных жестов: прошивка Xiaomi гасит его
 * при перезагрузке, поэтому загрузку телефона слушаем тоже.
 */
class RestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                NavGestures.apply(context)
                val home = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                runCatching { context.startActivity(home) }
            }

            Intent.ACTION_BOOT_COMPLETED -> NavGestures.apply(context)
        }
    }
}
