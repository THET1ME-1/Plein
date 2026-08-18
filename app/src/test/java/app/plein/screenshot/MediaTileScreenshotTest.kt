package app.plein.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.plein.ui.home.MediaTile
import org.junit.Test

/** Плитка «сейчас играет» во всех трёх размерах и в двух особых случаях. */
class MediaTileScreenshotTest : ScreenshotTest() {

    /** Обложка вместо настоящей: два цвета по диагонали, как у альбома. */
    private fun cover(): Bitmap {
        val size = 300
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                0xFF1F6F5C.toInt(), 0xFFD8B25A.toInt(), Shader.TileMode.CLAMP,
            )
        }
        Canvas(bitmap).drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return bitmap
    }

    @Test
    fun `плитка во всех размерах`() {
        val art = cover()
        snap("tile-media", dark = true) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(156.dp)) {
                    MediaTile("Полёт над городом", "Хмурый Ветер", art, true, true, {}, {}, {})
                }
                Box(Modifier.fillMaxWidth().height(74.dp)) {
                    MediaTile("Полёт над городом", "Хмурый Ветер", art, false, true, {}, {}, {})
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(156.dp)) {
                        MediaTile("Полёт над городом", "Хмурый Ветер", art, true, true, {}, {}, {})
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.width(190.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().height(72.dp)) {
                            MediaTile(null, "", null, false, true, {}, {}, {})
                        }
                        Box(Modifier.fillMaxWidth().height(72.dp)) {
                            MediaTile(null, "", null, false, false, {}, {}, {})
                        }
                    }
                }
            }
        }
    }
}
