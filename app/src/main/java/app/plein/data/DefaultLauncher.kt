package app.plein.data

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Назначение лаунчера по умолчанию.
 *
 * На Android 10+ система показывает свой диалог выбора роли, на старых
 * версиях остаётся открыть настройки домашнего экрана и выбрать руками.
 */
object DefaultLauncher {

    fun isDefault(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val current = context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        return current == context.packageName
    }

    /** Интент запроса: сначала роль, иначе настройки. */
    fun requestIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }
}
