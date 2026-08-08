package app.plein.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Подпись автора под кадром.
 *
 * У Openverse поле creator приходит пустым как JSON-null, а `optString`
 * возвращает на него строку «null» — она и попадала на экран.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JsonTextTest {

    @Test
    fun `json-null не превращается в слово null`() {
        val item = JSONObject("""{"creator": null, "license": "cc0"}""")
        assertEquals("", item.text("creator"))
        assertEquals("cc0", item.text("license"))
    }

    @Test
    fun `отсутствующее поле даёт пустоту`() {
        assertEquals("", JSONObject("{}").text("creator"))
    }
}
