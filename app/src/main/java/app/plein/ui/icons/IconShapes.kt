package app.plein.ui.icons

import android.graphics.Matrix
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.transformed

/**
 * Форма значка.
 *
 * Считается формулой через RoundedPolygon, а не рисуется руками: те же
 * фигуры, что в макете, и любая из них умеет перетекать в другую через Morph.
 */
enum class IconShape(val title: String, private val builder: () -> RoundedPolygon) {

    Default("По умолчанию", { RoundedPolygon.rectangle(1f, 1f, CornerRounding(0.34f, 0.6f)) }),
    Circle("Круг", { RoundedPolygon.circle(numVertices = 12) }),
    Square("Квадрат", { RoundedPolygon.rectangle(1f, 1f, CornerRounding(0.16f, 0.4f)) }),
    Clover("Цветок", { RoundedPolygon.star(4, 1f, 0.62f, CornerRounding(0.55f), CornerRounding(0.35f)) }),
    Cookie6("Печенье 6", { RoundedPolygon.star(6, 1f, 0.82f, CornerRounding(1f), CornerRounding(1f)) }),
    Cookie9("Печенье 9", { RoundedPolygon.star(9, 1f, 0.86f, CornerRounding(1f), CornerRounding(1f)) }),
    Sunny("Солнце", { RoundedPolygon.star(12, 1f, 0.9f, CornerRounding(1f), CornerRounding(1f)) }),
    Burst("Взрыв", { RoundedPolygon.star(8, 1f, 0.62f, CornerRounding(0.12f), CornerRounding(0.12f)) }),
    Flower("Ромашка", { RoundedPolygon.star(8, 1f, 0.68f, CornerRounding(0.9f), CornerRounding(0.45f)) }),
    Gem("Самоцвет", { RoundedPolygon(numVertices = 8, rounding = CornerRounding(0.22f)) }),
    Pentagon("Пятиугольник", { RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.28f)) }),
    Diamond("Ромб", { RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.22f)) }),
    Triangle("Треугольник", { RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.32f)).rotate(-90f) }),
    Pill("Пилюля", { RoundedPolygon.pill(width = 1f, height = 0.72f) });

    private val shapeCache: Shape by lazy { PolygonShape(builder().normalized()) }

    fun shape(): Shape = if (this == Circle) CircleShape else shapeCache

    companion object {
        /** Четыре формы на виду в настройках, остальные прячутся под «Больше». */
        val featured = listOf(Default, Circle, Square, Clover)
    }
}

private fun RoundedPolygon.rotate(degrees: Float): RoundedPolygon =
    transformed(Matrix().apply { setRotate(degrees) })

/** RoundedPolygon как Shape для Compose: путь считается один раз на размер. */
private class PolygonShape(private val polygon: RoundedPolygon) : Shape {

    private var cachedSize: Size = Size.Unspecified
    private var cachedPath: Path = Path()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size != cachedSize) {
            cachedPath = polygon.toPath().asComposePath().apply {
                transform(
                    androidx.compose.ui.graphics.Matrix().apply {
                        scale(size.width, size.height)
                    }
                )
            }
            cachedSize = size
        }
        return Outline.Generic(cachedPath)
    }
}
