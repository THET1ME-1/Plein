package app.plein.data

/**
 * Каталог Google Fonts.
 *
 * Шрифты не скачиваются в сборку: Compose берёт их через провайдер Play
 * Services по имени семейства. Каталог здесь нужен только для поиска, поэтому
 * это список имён, а не файлы.
 */
object FontCatalog {

    /** Пустая строка означает шрифт ДНК, Unbounded и Onest. */
    const val DEFAULT = ""

    val families = listOf(
        "Inter", "Manrope", "Montserrat", "Nunito", "Onest", "Unbounded",
        "Rubik", "Raleway", "Oswald", "Lora", "Merriweather", "Playfair Display",
        "Roboto", "Roboto Mono", "Roboto Serif", "Roboto Flex", "Open Sans",
        "Source Sans 3", "Source Serif 4", "IBM Plex Sans", "IBM Plex Mono",
        "IBM Plex Serif", "JetBrains Mono", "Fira Sans", "Fira Code", "Space Grotesk",
        "Space Mono", "DM Sans", "DM Serif Display", "DM Mono", "Poppins", "Quicksand",
        "Comfortaa", "Exo 2", "Jura", "Cormorant", "EB Garamond", "Bitter", "Karla",
        "Work Sans", "Public Sans", "Outfit", "Figtree", "Sora", "Syne", "Epilogue",
        "Chivo", "Archivo", "Archivo Narrow", "Bebas Neue", "Anton", "Alegreya",
        "Cardo", "Vollkorn", "Spectral", "Literata", "Newsreader", "Petrona",
        "Marck Script", "Caveat", "Pacifico", "Lobster", "Ruslan Display",
        "Yeseva One", "Podkova", "Alumni Sans", "Commissioner", "Golos Text",
        "PT Sans", "PT Serif", "PT Mono", "Ubuntu", "Ubuntu Mono", "Noto Sans",
        "Noto Serif", "Cousine", "Tinos", "Arimo", "Barlow", "Cabin", "Catamaran",
        "Dosis", "Heebo", "Hind", "Josefin Sans", "Kanit", "Lato", "Libre Baskerville",
        "Libre Franklin", "Mulish", "Overpass", "Prompt", "Red Hat Display",
        "Red Hat Text", "Rokkitt", "Signika", "Titillium Web", "Urbanist", "Zilla Slab",
    ).sorted()

    fun search(query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return families
        return families.filter { it.contains(trimmed, ignoreCase = true) }
    }
}
