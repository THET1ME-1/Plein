package app.plein.data

/**
 * Стопки редких приложений.
 *
 * Лаунчер сам складывает в одну клетку то, что открывают раз в месяц: экран
 * перестаёт зарастать. Частые приложения не трогаем никогда — стопка нужна
 * для хвоста, а не для того, чтобы прятать нужное.
 *
 * Работает только когда включено в настройках: молча перекладывать чужие
 * значки лаунчер не вправе.
 */
object Stacks {

    /** Сколько приложений влезает в одну стопку. */
    const val SIZE = 4

    /** Реже этой доли от самого частого — считается редким. */
    private const val RARE_SHARE = 0.15

    data class Group(val keys: List<String>)

    /**
     * Разложить список на одиночек и стопки.
     *
     * Возвращает пару: что осталось значками и что ушло в стопки. Порядок
     * одиночек сохраняем — человек его сам выставлял.
     */
    fun split(
        keys: List<String>,
        launches: (String) -> Int,
        enabled: Boolean,
    ): Pair<List<String>, List<Group>> {
        if (!enabled || keys.size <= SIZE) return keys to emptyList()

        val top = keys.maxOfOrNull(launches) ?: 0
        if (top <= 0) return keys to emptyList()

        val threshold = top * RARE_SHARE
        val rare = keys.filter { launches(it) <= threshold }
        // Меньше полной стопки прятать незачем: одна клетка вместо одной.
        if (rare.size < SIZE) return keys to emptyList()

        val singles = keys.filterNot { it in rare }
        val groups = rare.chunked(SIZE).filter { it.size == SIZE }.map { Group(it) }
        val leftover = rare.drop(groups.size * SIZE)
        return (singles + leftover) to groups
    }
}
