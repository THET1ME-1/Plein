package app.plein.data

/**
 * Живая папка: собирается по правилу и дальше следит за собой сама.
 *
 * Поставили новую игру — она легла в «Игры» без спроса, удалили — пропала.
 * А вот ручная правка сильнее правила: вынул приложение из папки — обратно
 * оно не вернётся, иначе лаунчер спорит с хозяином. Порядок, выставленный
 * руками, тоже неприкосновенен: новое уходит в конец.
 */
object FolderRules {

    fun apply(
        current: List<String>,
        matching: List<String>,
        removed: Set<String>,
        keepStrangers: Boolean = false,
    ): List<String> {
        val allowed = matching.toSet()
        val kept = current.filter { key -> key !in removed && (keepStrangers || key in allowed) }
        val added = matching.filter { it !in kept && it !in removed }
        return kept + added
    }
}
