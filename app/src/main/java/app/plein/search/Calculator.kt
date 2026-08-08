package app.plein.search

/**
 * Счёт прямо в строке поиска: 12*7, 15% от 240, 2+2*2.
 * Рекурсивный спуск, потому что тащить движок ради арифметики незачем.
 */
object Calculator {

    fun evaluate(input: String): Double? {
        val cleaned = input.replace(',', '.').replace('×', '*').replace('÷', '/').trim()
        if (cleaned.isEmpty()) return null
        if (!cleaned.any { it.isDigit() }) return null
        if (!cleaned.any { it in "+-*/%(" }) return null
        return runCatching { Parser(cleaned).parse() }.getOrNull()?.takeIf { it.isFinite() }
    }

    fun format(value: Double): String {
        val rounded = Math.round(value * 1_000_000.0) / 1_000_000.0
        return if (rounded == Math.floor(rounded) && !rounded.isInfinite()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private class Parser(private val text: String) {
        private var pos = 0

        fun parse(): Double {
            val value = expression()
            skipSpaces()
            if (pos < text.length) throw IllegalArgumentException("лишние символы")
            return value
        }

        private fun expression(): Double {
            var result = term()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '+' -> { pos++; result += term() }
                    '-' -> { pos++; result -= term() }
                    else -> return result
                }
            }
        }

        private fun term(): Double {
            var result = factor()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '*' -> { pos++; result *= factor() }
                    '/' -> { pos++; result /= factor() }
                    '%' -> { pos++; result /= 100.0 }
                    else -> return result
                }
            }
        }

        private fun factor(): Double {
            skipSpaces()
            when (peek()) {
                '-' -> { pos++; return -factor() }
                '+' -> { pos++; return factor() }
                '(' -> {
                    pos++
                    val inner = expression()
                    skipSpaces()
                    if (peek() != ')') throw IllegalArgumentException("нет закрывающей скобки")
                    pos++
                    return inner
                }
            }
            return number()
        }

        private fun number(): Double {
            skipSpaces()
            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException("ожидалось число")
            return text.substring(start, pos).toDouble()
        }

        private fun peek(): Char? = if (pos < text.length) text[pos] else null

        private fun skipSpaces() {
            while (pos < text.length && text[pos] == ' ') pos++
        }
    }
}
