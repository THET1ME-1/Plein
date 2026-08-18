package app.plein.search

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** Человек из телефонной книги: имя, один номер и ключ карточки. */
data class Contact(
    val id: Long,
    val name: String,
    val phone: String,
    val lookup: String?,
    val photo: String? = null,
)

/**
 * Контакты в поиске.
 *
 * Имя ищется тем же `AppRanker`, что и названия приложений: транслит и чужая
 * раскладка достаются даром. Номер ищется цифрами, но не короче трёх — две
 * цифры совпадают с половиной книги.
 */
object Contacts {

    /** Короче этого числа цифры за номер не считаем. */
    private const val DIGITS = 3

    /** Совпадение по номеру: слабее любого совпадения по имени. */
    private const val BY_PHONE = 0.5

    fun granted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Из сырых строк книги — готовая выдача.
     *
     * У одного человека бывает три номера, и провайдер отдаёт три строки:
     * оставляем первую, остальные схлопываем по номеру карточки.
     */
    fun pick(rows: List<Contact>, query: String, limit: Int): List<Contact> {
        val digits = query.filter(Char::isDigit)
        val unique = LinkedHashMap<Long, Contact>()
        rows.forEach { row -> unique.putIfAbsent(row.id, row) }
        return unique.values
            .map { it to weightOf(it, query, digits) }
            .filter { it.second > 0.0 }
            .sortedWith(compareByDescending<Pair<Contact, Double>> { it.second }.thenBy { it.first.name })
            .take(limit)
            .map { it.first }
    }

    private fun weightOf(contact: Contact, query: String, digits: String): Double {
        val byName = AppRanker.matchOf(contact.name, query)
        if (byName > 0.0) return byName
        if (digits.length < DIGITS) return 0.0
        return if (contact.phone.filter(Char::isDigit).contains(digits)) BY_PHONE else 0.0
    }
}

/**
 * Чтение телефонной книги.
 *
 * Провайдер ищет по имени так, как оно записано, поэтому «vfif» он не поймёт:
 * второй заход идёт с подменённой раскладкой. Дальше выдачу разбирает `pick`.
 */
class ContactSearch(private val context: Context) {

    fun find(query: String, limit: Int = 4): List<Contact> {
        if (query.isBlank() || !Contacts.granted(context)) return emptyList()

        val rows = read(query) + read(AppRanker.swapLayout(query)).takeIf { query != AppRanker.swapLayout(query) }
            .orEmpty()
        return Contacts.pick(rows, query, limit)
    }

    private fun read(query: String): List<Contact> = runCatching {
        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(query),
        )
        val rows = mutableListOf<Contact>()
        context.contentResolver.query(uri, COLUMNS, null, null, null)?.use { cursor ->
            while (cursor.moveToNext() && rows.size < SCAN) {
                val name = cursor.getString(1) ?: continue
                rows += Contact(
                    id = cursor.getLong(0),
                    name = name,
                    phone = cursor.getString(2).orEmpty(),
                    lookup = cursor.getString(3),
                    photo = cursor.getString(4),
                )
            }
        }
        rows
    }.getOrDefault(emptyList())

    private companion object {
        /** Больше этого числа строк разбирать незачем: показываем единицы. */
        const val SCAN = 40

        val COLUMNS = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
        )
    }
}
