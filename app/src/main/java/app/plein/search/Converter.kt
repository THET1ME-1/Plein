package app.plein.search

import java.util.Locale

/**
 * Перевод величин прямо в строке поиска.
 *
 * Разбираем то, как пишут люди: «10 км в мили», «5 kg to lb», «100 F в C».
 * Единицы знают синонимы на двух языках, потому что человек набирает «км» и
 * «km» с одинаковой охотой.
 */
object Converter {

    data class Result(val value: Double, val unit: String, val source: String)

    private data class Unit(
        val id: String,
        val display: String,
        val family: String,
        /** Во сколько раз больше основной единицы семейства. */
        val factor: Double,
        val names: List<String>,
    )

    private val units = listOf(
        // Длина, основа метр
        Unit("m", "м", "length", 1.0, listOf("m", "м", "метр", "метра", "метров", "meter", "meters", "metre")),
        Unit("km", "км", "length", 1000.0, listOf("km", "км", "километр", "километра", "километров", "kilometer", "kilometers")),
        Unit("cm", "см", "length", 0.01, listOf("cm", "см", "сантиметр", "сантиметра", "сантиметров", "centimeter")),
        Unit("mm", "мм", "length", 0.001, listOf("mm", "мм", "миллиметр", "миллиметра", "millimeter")),
        Unit("mi", "мили", "length", 1609.344, listOf("mi", "миля", "мили", "миль", "mile", "miles")),
        Unit("ft", "футов", "length", 0.3048, listOf("ft", "фут", "фута", "футов", "foot", "feet")),
        Unit("in", "дюймов", "length", 0.0254, listOf("in", "дюйм", "дюйма", "дюймов", "inch", "inches")),
        Unit("yd", "ярдов", "length", 0.9144, listOf("yd", "ярд", "ярда", "ярдов", "yard", "yards")),
        Unit("nmi", "морских миль", "length", 1852.0, listOf("nmi", "морская миля", "nautical mile")),

        // Масса, основа килограмм
        Unit("kg", "кг", "mass", 1.0, listOf("kg", "кг", "килограмм", "килограмма", "килограммов", "kilogram", "kilograms")),
        Unit("g", "г", "mass", 0.001, listOf("g", "г", "гр", "грамм", "грамма", "граммов", "gram", "grams")),
        Unit("t", "т", "mass", 1000.0, listOf("t", "т", "тонна", "тонны", "тонн", "ton", "tonne", "tons")),
        Unit("lb", "фунтов", "mass", 0.45359237, listOf("lb", "lbs", "фунт", "фунта", "фунтов", "pound", "pounds")),
        Unit("oz", "унций", "mass", 0.028349523125, listOf("oz", "унция", "унции", "унций", "ounce", "ounces")),

        // Объём, основа литр
        Unit("l", "л", "volume", 1.0, listOf("l", "л", "литр", "литра", "литров", "liter", "litre", "liters")),
        Unit("ml", "мл", "volume", 0.001, listOf("ml", "мл", "миллилитр", "миллилитра", "milliliter")),
        Unit("gal", "галлонов", "volume", 3.785411784, listOf("gal", "галлон", "галлона", "галлонов", "gallon", "gallons")),

        // Площадь, основа квадратный метр
        Unit("m2", "м²", "area", 1.0, listOf("m2", "м2", "кв м", "квадратный метр", "square meter")),
        Unit("km2", "км²", "area", 1_000_000.0, listOf("km2", "км2", "кв км", "квадратный километр", "square kilometer")),
        Unit("ha", "га", "area", 10_000.0, listOf("ha", "га", "гектар", "гектара", "гектаров", "hectare")),
        Unit("acre", "акров", "area", 4046.8564224, listOf("acre", "acres", "акр", "акра", "акров")),

        // Скорость, основа метр в секунду
        Unit("kmh", "км/ч", "speed", 0.2777777778, listOf("kmh", "км/ч", "кмч", "km/h", "kph")),
        Unit("ms", "м/с", "speed", 1.0, listOf("ms", "м/с", "m/s")),
        Unit("mph", "миль/ч", "speed", 0.44704, listOf("mph", "миль/ч", "mi/h")),
        Unit("knot", "узлов", "speed", 0.514444, listOf("knot", "knots", "узел", "узла", "узлов")),

        // Данные, основа байт
        Unit("b", "Б", "data", 1.0, listOf("b", "byte", "bytes", "байт", "байта", "байтов")),
        Unit("kb", "КБ", "data", 1024.0, listOf("kb", "кб", "килобайт", "килобайта", "kilobyte")),
        Unit("mb", "МБ", "data", 1024.0 * 1024, listOf("mb", "мб", "мегабайт", "мегабайта", "megabyte")),
        Unit("gb", "ГБ", "data", 1024.0 * 1024 * 1024, listOf("gb", "гб", "гигабайт", "гигабайта", "gigabyte")),
        Unit("tb", "ТБ", "data", 1024.0 * 1024 * 1024 * 1024, listOf("tb", "тб", "терабайт", "терабайта", "terabyte")),

        // Время, основа секунда
        Unit("s", "с", "time", 1.0, listOf("s", "с", "сек", "секунда", "секунды", "секунд", "second", "seconds")),
        Unit("min", "мин", "time", 60.0, listOf("min", "мин", "минута", "минуты", "минут", "minute", "minutes")),
        Unit("h", "ч", "time", 3600.0, listOf("h", "ч", "час", "часа", "часов", "hour", "hours")),
        Unit("d", "дней", "time", 86400.0, listOf("d", "д", "день", "дня", "дней", "day", "days")),
    )

    /** Температура живёт отдельно: у неё сдвиг, а не множитель. */
    private val temperature = mapOf(
        "c" to listOf("c", "с", "цельсий", "цельсия", "celsius"),
        "f" to listOf("f", "фаренгейт", "фаренгейта", "fahrenheit"),
        "k" to listOf("k", "к", "кельвин", "кельвина", "kelvin"),
    )

    private val separators = listOf(" в ", " to ", " -> ", " → ", " into ", " у ", " до ")

    fun convert(input: String): Result? {
        val query = input.trim().lowercase(Locale.ROOT).replace(',', '.')
        val separator = separators.firstOrNull { query.contains(it) } ?: return null
        val left = query.substringBefore(separator).trim()
        val right = query.substringAfter(separator).trim()
        if (left.isEmpty() || right.isEmpty()) return null

        // Остаток берём по границе найденного числа: «10» и «10.0» пишутся
        // по-разному, и отрезать по строке самого числа нельзя.
        val number = Regex("^-?\\d+(\\.\\d+)?").find(left) ?: return null
        val amount = number.value.toDoubleOrNull() ?: return null
        val fromName = left.substring(number.range.last + 1).trim()

        temperatureOf(fromName)?.let { from ->
            val to = temperatureOf(right) ?: return null
            val celsius = when (from) {
                "f" -> (amount - 32) * 5 / 9
                "k" -> amount - 273.15
                else -> amount
            }
            val value = when (to) {
                "f" -> celsius * 9 / 5 + 32
                "k" -> celsius + 273.15
                else -> celsius
            }
            return Result(value, "°" + to.uppercase(Locale.ROOT), input)
        }

        val from = unitOf(fromName) ?: return null
        val to = unitOf(right) ?: return null
        if (from.family != to.family) return null
        return Result(amount * from.factor / to.factor, to.display, input)
    }

    private fun unitOf(name: String): Unit? {
        val clean = name.trim().trimEnd('.', '?')
        return units.firstOrNull { unit -> unit.names.any { it == clean } }
            ?: units.firstOrNull { unit -> unit.names.any { clean.startsWith(it) && clean.length - it.length <= 2 } }
    }

    private fun temperatureOf(name: String): String? {
        val clean = name.trim().removePrefix("°").trimEnd('.', '?')
        return temperature.entries.firstOrNull { (_, names) -> names.any { it == clean } }?.key
    }

    /** Красивое число: без хвоста нулей и без экспоненты на бытовых величинах. */
    fun format(value: Double): String = when {
        value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e12 -> value.toLong().toString()
        kotlin.math.abs(value) >= 1000 -> String.format(Locale.ROOT, "%.1f", value)
        kotlin.math.abs(value) >= 1 -> String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
        else -> String.format(Locale.ROOT, "%.4f", value).trimEnd('0').trimEnd('.')
    }
}
