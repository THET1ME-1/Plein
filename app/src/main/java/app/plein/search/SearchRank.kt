package app.plein.search

/**
 * Что стоит в ряду под пустой строкой поиска.
 *
 * Считаем не запуски вообще, а открытия именно из поиска. Приложение, до
 * которого человек каждый раз добирается набором букв, ему и нужно под рукой;
 * то, что и так лежит на домашнем экране, в этот ряд не лезет.
 */
object SearchRank {

    /** Сколько раз открыли из поиска и когда в последний раз. */
    data class Use(val count: Int, val lastAt: Long)

    /** Один заход это случайность, а не привычка. */
    private const val ENOUGH = 2

    /** Забытое за столько дней перестаёт занимать место. */
    private const val FORGET_DAYS = 90

    fun top(use: Map<String, Use>, limit: Int, now: Long = 0L): List<String> {
        val edge = if (now > 0) now - FORGET_DAYS * 24L * 3600_000L else Long.MIN_VALUE
        return use.entries
            .filter { it.value.count >= ENOUGH && it.value.lastAt >= edge }
            .sortedWith(
                compareByDescending<Map.Entry<String, Use>> { it.value.count }
                    .thenByDescending { it.value.lastAt }
                    .thenBy { it.key }
            )
            .take(limit)
            .map { it.key }
    }
}
