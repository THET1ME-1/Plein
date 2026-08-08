package app.plein.search

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Курсы валют.
 *
 * `open.er-api.com` отдаёт таблицу без ключа и без регистрации. Тянем её раз
 * в сутки и держим у себя: без сети лаунчер честно считает по вчерашнему
 * курсу и говорит, какого он числа, вместо того чтобы молчать.
 */
class Currency(private val context: Context) {

    data class Rate(val value: Double, val code: String, val updated: Long)

    private val sp = context.getSharedPreferences("plein_rates", Context.MODE_PRIVATE)

    private val names = mapOf(
        "usd" to listOf("usd", "$", "долл", "доллар", "доллара", "долларов", "dollar", "dollars", "бакс"),
        "eur" to listOf("eur", "€", "евро", "euro"),
        "rub" to listOf("rub", "₽", "руб", "рубль", "рубля", "рублей", "ruble"),
        "mdl" to listOf("mdl", "лей", "лея", "леев", "leu", "lei"),
        "uah" to listOf("uah", "₴", "грн", "гривна", "гривны", "гривен", "hryvnia"),
        "gbp" to listOf("gbp", "£", "фунт стерлингов", "pound sterling"),
        "pln" to listOf("pln", "злотый", "злотых", "zloty"),
        "try" to listOf("try", "лира", "лиры", "lira"),
        "kzt" to listOf("kzt", "тенге", "tenge"),
        "cny" to listOf("cny", "юань", "юаня", "юаней", "yuan"),
        "jpy" to listOf("jpy", "иена", "иены", "yen"),
        "chf" to listOf("chf", "франк", "франка", "franc"),
    )

    private val separators = listOf(" в ", " to ", " -> ", " → ", " у ", " до ")

    /** Разбор строки. Возвращает пару валют и сумму, если это про деньги. */
    fun parse(input: String): Triple<Double, String, String>? {
        val query = input.trim().lowercase(Locale.ROOT).replace(',', '.')
        val separator = separators.firstOrNull { query.contains(it) } ?: return null
        val left = query.substringBefore(separator).trim()
        val right = query.substringAfter(separator).trim()

        val number = Regex("^-?\\d+(\\.\\d+)?").find(left) ?: return null
        val amount = number.value.toDoubleOrNull() ?: return null
        val from = codeOf(left.substring(number.range.last + 1).trim()) ?: return null
        val to = codeOf(right) ?: return null
        if (from == to) return null
        return Triple(amount, from, to)
    }

    suspend fun rate(from: String, to: String): Rate? = withContext(Dispatchers.IO) {
        val cached = cachedTable(from)
        val fresh = if (cached == null || outdated(from)) fetch(from) ?: cached else cached
        val table = fresh ?: return@withContext null
        val value = table.optDouble(to.uppercase(Locale.ROOT), Double.NaN)
        if (value.isNaN()) return@withContext null
        Rate(value, to.uppercase(Locale.ROOT), sp.getLong(stamp(from), 0L))
    }

    private fun codeOf(name: String): String? {
        val clean = name.trim().trimEnd('.', '?')
        return names.entries.firstOrNull { (_, list) -> list.any { it == clean } }?.key
            ?: names.entries.firstOrNull { (_, list) -> list.any { clean.startsWith(it) } }?.key
    }

    private fun outdated(from: String): Boolean {
        val day = 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - sp.getLong(stamp(from), 0L) > day
    }

    private fun cachedTable(from: String): JSONObject? = runCatching {
        sp.getString(table(from), null)?.let { JSONObject(it) }
    }.getOrNull()

    private fun fetch(from: String): JSONObject? = runCatching {
        val url = URL("https://open.er-api.com/v6/latest/${from.uppercase(Locale.ROOT)}")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 12000
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val rates = JSONObject(body).optJSONObject("rates") ?: return null
        sp.edit()
            .putString(table(from), rates.toString())
            .putLong(stamp(from), System.currentTimeMillis())
            .apply()
        rates
    }.getOrNull()

    private fun table(from: String) = "rates_${from.lowercase(Locale.ROOT)}"

    private fun stamp(from: String) = "stamp_${from.lowercase(Locale.ROOT)}"
}
