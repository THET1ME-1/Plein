package app.plein.data

/**
 * Папка «Сейчас».
 *
 * Лаунчер и так помнит, в какой час дня что открывают: эта статистика держит
 * порядок в поиске. Отсюда собирается папка, которая утром показывает почту,
 * вечером плеер, а ночью читалку. Настраивать её нечем и незачем — она
 * описывает не замысел, а привычку.
 *
 * Час важнее общего счёта: приложение, открытое в это время трижды, стоит
 * выше того, что запускают сто раз в сутки, но в другое время.
 */
object NowFolder {

    /** Сколько влезает в папку: дальше идёт хвост, который никто не открывает. */
    const val SIZE = 12

    data class Candidate(val key: String, val atHour: Int, val total: Int)

    fun pick(candidates: List<Candidate>, limit: Int = SIZE): List<String> = candidates
        .filter { it.atHour > 0 || it.total > 0 }
        .sortedWith(compareByDescending<Candidate> { it.atHour }.thenByDescending { it.total }.thenBy { it.key })
        .take(limit)
        .map { it.key }
}
