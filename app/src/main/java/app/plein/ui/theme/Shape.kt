package app.plein.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Формы ДНК: карточки и листы 28, поля 18, снекбар 16.
 * Кнопки всегда пилюлей, поэтому им shape задаётся на месте.
 */
val PleinShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val SheetCorner = 30.dp
val CardCorner = 28.dp
val FieldCorner = 18.dp
