package app.plein.search

import android.net.Uri

/** Куда уходит запрос наружу. Свой шаблон тоже возможен: %s заменяется словом. */
object WebSearch {

    val providers = listOf("google", "ddg", "bing", "yandex", "startpage")

    fun url(provider: String, query: String): String {
        val encoded = Uri.encode(query)
        return when (provider) {
            "ddg" -> "https://duckduckgo.com/?q=$encoded"
            "bing" -> "https://www.bing.com/search?q=$encoded"
            "yandex" -> "https://yandex.ru/search/?text=$encoded"
            "startpage" -> "https://www.startpage.com/sp/search?query=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }
}
