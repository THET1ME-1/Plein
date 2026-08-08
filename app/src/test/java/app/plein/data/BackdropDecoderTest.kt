package app.plein.data

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Декодер кадра.
 *
 * Прошлая версия возвращала null всегда: проверка стояла на результате чтения
 * границ, а он при `inJustDecodeBounds` пустой по определению. Фон оставался
 * серым, хотя фотография лежала на диске и подпись автора была на месте.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BackdropDecoderTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun shot(width: Int, height: Int): File {
        val file = File.createTempFile("shot", ".jpg")
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { source ->
            file.outputStream().use { source.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }
        return file
    }

    @Test
    fun `кадр с диска доходит до экрана`() {
        val backdrop = Backdrop(file = shot(2400, 3600), author = "", credit = "", luminance = 0.4f)
        val decoded = BackdropDecoder.forScreen(context, backdrop, screenWidth = 1080)

        assertNotNull("кадр не декодировался, фон остался пустым", decoded)
        assertEquals(2400, decoded!!.width)
    }

    @Test
    fun `огромный кадр ужимается под экран`() {
        val backdrop = Backdrop(file = shot(5000, 7000), author = "", credit = "", luminance = 0.4f)
        val decoded = BackdropDecoder.forScreen(context, backdrop, screenWidth = 1080)

        assertNotNull(decoded)
        // Полтора экрана в запасе: дальше уменьшать нельзя, поедет резкость.
        assertTrue("кадр не ужат: ${decoded!!.width}", decoded.width <= 1620 * 2)
        assertTrue("кадр ужат слишком: ${decoded.width}", decoded.width >= 1080)
    }

    @Test
    fun `битый файл не роняет экран`() {
        val broken = File.createTempFile("broken", ".jpg").apply { writeText("не картинка") }
        val backdrop = Backdrop(file = broken, author = "", credit = "", luminance = 0.4f)

        assertEquals(null, BackdropDecoder.forScreen(context, backdrop, screenWidth = 1080))
    }

    @Test
    fun `степень уменьшения растёт с размером`() {
        assertEquals(1, BackdropDecoder.sampleFor(sourceWidth = 1080, screenWidth = 1080))
        assertEquals(1, BackdropDecoder.sampleFor(sourceWidth = 2400, screenWidth = 1080))
        assertEquals(2, BackdropDecoder.sampleFor(sourceWidth = 4000, screenWidth = 1080))
        assertEquals(4, BackdropDecoder.sampleFor(sourceWidth = 8000, screenWidth = 1080))
    }
}
