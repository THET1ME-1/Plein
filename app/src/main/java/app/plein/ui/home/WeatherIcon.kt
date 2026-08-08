package app.plein.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/** Код WMO в значок Material. Эмодзи в интерфейсе не место. */
fun weatherIcon(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Rounded.WbSunny
    2, 3 -> Icons.Rounded.Cloud
    in 45..48 -> Icons.Rounded.BlurOn
    in 51..67 -> Icons.Rounded.WaterDrop
    in 71..77 -> Icons.Rounded.AcUnit
    in 80..82 -> Icons.Rounded.WaterDrop
    in 95..99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.Cloud
}
