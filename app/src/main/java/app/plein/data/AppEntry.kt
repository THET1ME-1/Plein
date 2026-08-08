package app.plein.data

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.os.UserHandle

/** Одно запускаемое приложение. key уникален с учётом рабочего профиля. */
data class AppEntry(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val category: Int = ApplicationInfo.CATEGORY_UNDEFINED,
    val system: Boolean = false,
    val customLabel: String? = null,
) {
    val key: String get() = "${component.flattenToShortString()}#${user.hashCode()}"
    val title: String get() = customLabel ?: label
}

/** Страница домашнего экрана: подборка приложений. */
data class Folder(
    val title: String,
    val apps: List<AppEntry>,
)
