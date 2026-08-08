package app.plein.search

import java.util.Locale

/**
 * Порядок приложений в поиске.
 *
 * Три вещи разом. Первая: набирают по-разному — «те» с начала, «нст» первыми
 * буквами слов, «гр» из середины. Вторая: то, что открывают чаще, стоит выше.
 * Третья: лаунчер помнит час, в который приложение обычно открывают, и утром
 * поднимает почту, а вечером плеер.
 *
 * Совпадение важнее привычки: человек набрал буквы не просто так. Привычка
 * решает спор между одинаково подходящими.
 */
object AppRanker {

    /** Совпадения по убыванию силы. */
    private const val PREFIX = 1.0
    private const val WORD_START = 0.8
    private const val INITIALS = 0.65
    private const val CONTAINS = 0.5
    private const val SUBSEQUENCE = 0.3

    fun score(
        title: String,
        query: String,
        launches: Int,
        launchesAtHour: Int,
        totalLaunches: Int,
    ): Double {
        val habit = habitOf(launches, launchesAtHour, totalLaunches)
        if (query.isBlank()) return habit

        val match = matchOf(title, query)
        if (match == 0.0) return 0.0
        // Привычка добавляет не больше трети: она уточняет, а не подменяет.
        return match + habit * 0.33
    }

    /**
     * Насколько запрос похож на название. Ноль — не похож вовсе.
     *
     * Обе строки приводим к латинице: набирают по-русски, а половина названий
     * записана латиницей. «гк» должно находить Google Карты, «тг» — Telegram.
     */
    fun matchOf(title: String, query: String): Double {
        val needle = translit(query.trim().lowercase(Locale.getDefault()))
        if (needle.isEmpty()) return 0.0
        val name = translit(title.lowercase(Locale.getDefault()))

        if (name.startsWith(needle)) return PREFIX

        val words = name.split(' ', '-', '_', '.').filter { it.isNotEmpty() }
        if (words.any { it.startsWith(needle) }) return WORD_START

        // Первые буквы слов: «гк» находит «Google Карты».
        val initials = words.map { it.first() }.joinToString("")
        if (initials.startsWith(needle)) return INITIALS

        if (name.contains(needle)) return CONTAINS

        // Буквы подряд, но с пропусками: «нст» находит «Настройки».
        return if (isSubsequence(needle, name)) SUBSEQUENCE else 0.0
    }

    /** Привычка: как часто открывают вообще и как часто в этот час. */
    private fun habitOf(launches: Int, launchesAtHour: Int, totalLaunches: Int): Double {
        if (totalLaunches <= 0) return 0.0
        val overall = launches.toDouble() / totalLaunches
        val byHour = if (launches <= 0) 0.0 else launchesAtHour.toDouble() / launches
        return overall * 0.6 + byHour * 0.4
    }

    /**
     * Кириллица в латиницу по звучанию.
     *
     * Не транслитерация по ГОСТу, а то, как человек набирает: «щ» и «ш» тут
     * равны, потому что искать он будет и так, и так.
     */
    fun translit(text: String): String {
        val builder = StringBuilder(text.length)
        text.forEach { letter ->
            val replacement = TRANSLIT[letter]
            if (replacement != null) builder.append(replacement) else builder.append(letter)
        }
        return builder.toString()
    }

    private val TRANSLIT = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "e",
        'ж' to "j", 'з' to "z", 'и' to "i", 'й' to "i", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "h", 'ц' to "c", 'ч' to "c", 'ш' to "s", 'щ' to "s", 'ъ' to "",
        'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "u", 'я' to "a",
    )

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        var index = 0
        haystack.forEach { letter ->
            if (index < needle.length && letter == needle[index]) index++
        }
        return index == needle.length
    }

    /**
     * Готовый порядок.
     *
     * Принимает пары «название — сколько раз открывали» и функцию, которая
     * знает про часы. Так ранжирование остаётся чистым и проверяемым.
     */
    fun rank(
        items: List<Pair<String, Int>>,
        query: String,
        hour: Int,
        launchesAt: (String, Int) -> Int,
        total: Int,
    ): List<String> = items
        .map { (title, launches) ->
            title to score(
                title = title,
                query = query,
                launches = launches,
                launchesAtHour = launchesAt(title, hour),
                totalLaunches = total,
            )
        }
        .filter { it.second > 0.0 }
        .sortedWith(compareByDescending<Pair<String, Double>> { it.second }.thenBy { it.first })
        .map { it.first }
}
