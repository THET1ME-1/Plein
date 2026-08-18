package app.plein.data

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Кэш значков на диске.
 *
 * Запись шла прямо в целевой файл, а прогрев идёт в два потока параллельно с
 * отрисовкой видимых значков: на одном ключе оба писали в один файл, и на
 * диске оставался обрубок PNG. Кэш лечился сам — чтение отбрасывало битое, —
 * но с миганием значка и повторной отрисовкой.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class IconDiskCacheTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dir = File(context.cacheDir, "icons")
    private lateinit var cache: IconDiskCache

    @Before
    fun setUp() {
        dir.deleteRecursively()
        cache = IconDiskCache(context)
    }

    private fun icon(): Bitmap = createBitmap(8, 8).apply { eraseColor(0xFF204060.toInt()) }

    @Test
    fun `записанный значок читается обратно`() {
        cache.write("app.plein", "app.plein/Main@96@circle@@color", icon())
        val read = cache.read("app.plein", "app.plein/Main@96@circle@@color")
        assertNotNull(read)
        assertEquals(8, read!!.width)
    }

    @Test
    fun `недописанных хвостов на диске не остаётся`() {
        cache.write("app.plein", "ключ", icon())
        val leftovers = dir.listFiles()?.filter { it.name.endsWith(".tmp") }.orEmpty()
        assertTrue("временный файл остался: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun `битый файл выбрасывается при чтении`() {
        cache.write("app.plein", "ключ", icon())
        val file = dir.listFiles()!!.first { it.name.endsWith(".png") }
        file.writeBytes(byteArrayOf(1, 2, 3))

        assertNull(cache.read("app.plein", "ключ"))
        assertFalse("обрубок должен быть удалён", file.exists())
    }

    @Test
    fun `обновление пакета стирает только его значки`() {
        cache.write("app.plein", "свой", icon())
        cache.write("com.other", "чужой", icon())

        cache.forget("app.plein")

        assertNull(cache.read("app.plein", "свой"))
        assertNotNull(cache.read("com.other", "чужой"))
    }

    @Test
    fun `чистка сносит хвосты от убитого процесса`() {
        cache.write("app.plein", "ключ", icon())
        File(dir, "app_plein-deadbeef.png.7.tmp").writeBytes(byteArrayOf(9))

        cache.trim()

        val leftovers = dir.listFiles()?.filter { it.name.endsWith(".tmp") }.orEmpty()
        assertTrue("хвост пережил чистку: $leftovers", leftovers.isEmpty())
        assertNotNull("целый значок чистка трогать не должна", cache.read("app.plein", "ключ"))
    }
}
