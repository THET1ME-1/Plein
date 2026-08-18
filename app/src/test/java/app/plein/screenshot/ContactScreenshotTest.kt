package app.plein.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.plein.search.Contact
import app.plein.ui.search.ContactRow
import org.junit.Assert.assertEquals
import org.junit.Test

/** Строка контакта в поиске: аватарка, номер, звонок и сообщение. */
class ContactScreenshotTest : ScreenshotTest() {

    @Test
    fun `строки контактов`() {
        val people = listOf(
            Contact(1, "Маша", "+373 79 000 001", "key1"),
            Contact(2, "Дядя Ваня с длинным именем", "+373 60 111 222", "key2"),
        )

        snap("search-contacts") {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                people.forEach { person ->
                    ContactRow(person = person, onOpen = {}, onCall = {}, onWrite = {})
                }
            }
        }

        assertEquals("Маша", people.first().name)
    }
}
