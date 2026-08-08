package app.plein.data

import android.content.pm.ApplicationInfo

/**
 * Страницы домашнего экрана.
 *
 * Первая всегда «Все приложения», дальше подборки по категории из манифеста
 * приложения. Категория заявлена самим разработчиком, поэтому папки получаются
 * осмысленными без ручной сортировки. Пустые и куцые подборки не показываем.
 */
object Folders {

    private const val MIN_APPS_IN_FOLDER = 4

    private val categories = listOf(
        ApplicationInfo.CATEGORY_SOCIAL to "Общение",
        ApplicationInfo.CATEGORY_AUDIO to "Музыка",
        ApplicationInfo.CATEGORY_VIDEO to "Видео",
        ApplicationInfo.CATEGORY_IMAGE to "Фото",
        ApplicationInfo.CATEGORY_PRODUCTIVITY to "Работа",
        ApplicationInfo.CATEGORY_GAME to "Игры",
        ApplicationInfo.CATEGORY_NEWS to "Новости",
        ApplicationInfo.CATEGORY_MAPS to "Карты",
    )

    fun build(apps: List<AppEntry>): List<Folder> {
        val pages = mutableListOf(Folder("Все приложения", apps))
        categories.forEach { (category, title) ->
            val inCategory = apps.filter { it.category == category }
            if (inCategory.size >= MIN_APPS_IN_FOLDER) pages += Folder(title, inCategory)
        }
        return pages
    }
}
