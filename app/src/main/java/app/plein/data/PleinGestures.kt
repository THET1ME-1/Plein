package app.plein.data

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Служба для действий, которых у стороннего лаунчера нет.
 *
 * Блокировка экрана и шторка отданы системе, и достучаться до них можно двумя
 * путями: правами администратора устройства или службой доступности. Берём
 * вторую: администратор даёт лишнее — стирание телефона, пароли, — а нам
 * нужны ровно два глобальных действия.
 *
 * Служба ничего не слушает и не читает: события ей не приходят вовсе, она
 * только выполняет то, о чём просит сам лаунчер.
 */
class PleinGestures : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private var instance: PleinGestures? = null

        fun connected(): Boolean = instance != null

        /** Включена ли служба в системе: список лежит в защищённых настройках. */
        fun enabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            val name = ComponentName(context, PleinGestures::class.java).flattenToString()
            return enabled.split(':').any { it.equals(name, ignoreCase = true) }
        }

        fun settingsIntent(): Intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        fun lockScreen(): Boolean {
            val service = instance ?: return false
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        fun notifications(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
    }
}
