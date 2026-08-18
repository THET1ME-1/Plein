package app.plein.theme

import app.plein.ui.theme.SeedChoice
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Каким цветом красить экран.
 *
 * Цвет считается из кадра и живёт отдельно от ручного: человек может выключить
 * «брать из кадра», подобрать свой, потом вернуть — и цвет кадра не должен
 * пропасть, а ручной не должен затереться.
 */
class SeedChoiceTest {

    private val fromPhoto = 0xFF3E7A52.toInt()
    private val manual = 0xFF8A4B2A.toInt()

    @Test
    fun `с кадра берём цвет кадра`() {
        assertEquals(fromPhoto, SeedChoice.of(fromPhoto = true, photo = fromPhoto, manual = manual))
    }

    @Test
    fun `свой цвет перебивает кадр`() {
        assertEquals(manual, SeedChoice.of(fromPhoto = false, photo = fromPhoto, manual = manual))
    }

    @Test
    fun `пока кадра не было, берём ручной`() {
        assertEquals(manual, SeedChoice.of(fromPhoto = true, photo = 0, manual = manual))
    }
}
