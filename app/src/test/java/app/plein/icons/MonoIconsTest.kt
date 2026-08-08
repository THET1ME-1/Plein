package app.plein.icons

import android.graphics.Bitmap
import android.graphics.Color
import app.plein.data.MonoIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Значки одним тоном.
 *
 * Линейные паки вроде Arcticons рисуют одним цветом на прозрачном фоне. Если
 * считать им силуэт по яркости, светлые линии посчитаются фоном и пропадут —
 * поэтому такие значки красятся по форме.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MonoIconsTest {

    private fun lineIcon(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        for (x in 8 until 24) {
            bitmap.setPixel(x, 16, color)
            bitmap.setPixel(16, x, color)
        }
        return bitmap
    }

    private fun colourfulIcon(): Bitmap {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        for (x in 0 until 32) for (y in 0 until 32) {
            bitmap.setPixel(x, y, Color.argb(255, x * 8 % 256, y * 8 % 256, 90))
        }
        return bitmap
    }

    @Test
    fun `линейный значок считается одноцветным`() {
        assertTrue(MonoIcons.isFlat(lineIcon(Color.WHITE)))
        assertTrue(MonoIcons.isFlat(lineIcon(Color.BLACK)))
    }

    @Test
    fun `цветной значок одноцветным не считается`() {
        assertFalse(MonoIcons.isFlat(colourfulIcon()))
    }

    @Test
    fun `маска сохраняет форму белых линий`() {
        val mask = MonoIcons.alphaMask(lineIcon(Color.WHITE))
        // Там, где была линия, маска непрозрачна; в пустоте — прозрачна.
        assertEquals(255, Color.alpha(mask.getPixel(16, 16)))
        assertEquals(0, Color.alpha(mask.getPixel(2, 2)))
    }

    @Test
    fun `маска сохраняет форму и у тёмных линий`() {
        val mask = MonoIcons.alphaMask(lineIcon(Color.BLACK))
        assertEquals(255, Color.alpha(mask.getPixel(16, 16)))
        assertEquals(0, Color.alpha(mask.getPixel(30, 30)))
    }
}
