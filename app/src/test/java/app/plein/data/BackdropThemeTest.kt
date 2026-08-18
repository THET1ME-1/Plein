package app.plein.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Когда идти за новым кадром из-за темы.
 *
 * Смена режима — повод показать новую фотографию. А вот первый заход после
 * запуска поводом не является: там уже лежит сохранённый кадр, и лезть за
 * новым на каждом старте лаунчер не должен.
 */
class BackdropThemeTest {

    @Test
    fun `первый заход поводом не считается`() {
        assertFalse(Backdrops.themeChanged(was = null, now = true))
        assertFalse(Backdrops.themeChanged(was = null, now = false))
    }

    @Test
    fun `та же тема кадр не трогает`() {
        assertFalse(Backdrops.themeChanged(was = true, now = true))
        assertFalse(Backdrops.themeChanged(was = false, now = false))
    }

    @Test
    fun `смена режима зовёт новый кадр`() {
        assertTrue(Backdrops.themeChanged(was = false, now = true))
        assertTrue(Backdrops.themeChanged(was = true, now = false))
    }
}
