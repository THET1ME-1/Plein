package app.plein.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.plein.search.Converter
import app.plein.ui.search.SearchPreviewCard
import org.junit.Assert.assertEquals
import org.junit.Test

/** Карточки ответов в поиске: перевод величин и курс валют. */
class SearchScreenshotTest : ScreenshotTest() {

    @Test
    fun `карточки ответа`() {
        val length = Converter.convert("10 км в мили")!!
        val temperature = Converter.convert("100 f в c")!!

        snap("search-cards") {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                SearchPreviewCard(
                    left = length.source,
                    right = "${Converter.format(length.value)} ${length.unit}",
                )
                SearchPreviewCard(
                    left = temperature.source,
                    right = "${Converter.format(temperature.value)} ${temperature.unit}",
                )
                SearchPreviewCard(
                    left = "100 долларов в леи",
                    right = "1770 MDL",
                    note = "курс на 8 авг",
                )
            }
        }

        assertEquals("6.21", Converter.format(length.value))
    }
}
