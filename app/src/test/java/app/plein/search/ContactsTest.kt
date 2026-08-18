package app.plein.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Контакты в поиске.
 *
 * У одного человека бывает три номера, а в выдаче он должен стоять один раз.
 * Имя ищется так же, как названия приложений: с транслитом и с чужой
 * раскладкой. Номер ищется цифрами.
 */
class ContactsTest {

    private fun contact(id: Long, name: String, phone: String) =
        Contact(id = id, name = name, phone = phone, lookup = "key$id")

    @Test
    fun `человек с тремя номерами стоит в выдаче один раз`() {
        val rows = listOf(
            contact(1, "Маша", "+37379000001"),
            contact(1, "Маша", "+37379000002"),
            contact(1, "Маша", "+37379000003"),
        )
        assertEquals(1, Contacts.pick(rows, "маша", limit = 5).size)
    }

    @Test
    fun `начало имени выше середины`() {
        val rows = listOf(contact(1, "Дядя Ваня", "1"), contact(2, "Ваня", "2"))
        assertEquals("Ваня", Contacts.pick(rows, "ван", limit = 5).first().name)
    }

    @Test
    fun `имя в чужой раскладке находится`() {
        val rows = listOf(contact(1, "Маша", "1"))
        assertEquals(1, Contacts.pick(rows, "vfif", limit = 5).size)
    }

    @Test
    fun `номер ищется цифрами`() {
        val rows = listOf(contact(1, "Маша", "+373 79 000 001"), contact(2, "Петя", "+373 60 111 222"))
        val found = Contacts.pick(rows, "79000", limit = 5)
        assertEquals(listOf("Маша"), found.map { it.name })
    }

    @Test
    fun `две цифры номером не считаются`() {
        val rows = listOf(contact(1, "Маша", "+37379000001"))
        assertTrue(Contacts.pick(rows, "37", limit = 5).isEmpty())
    }

    @Test
    fun `чужой запрос не находит никого`() {
        val rows = listOf(contact(1, "Маша", "1"), contact(2, "Петя", "2"))
        assertTrue(Contacts.pick(rows, "щщщ", limit = 5).isEmpty())
    }

    @Test
    fun `лимит режет хвост`() {
        val rows = (1L..10L).map { contact(it, "Маша $it", "$it") }
        assertEquals(3, Contacts.pick(rows, "маша", limit = 3).size)
    }
}
