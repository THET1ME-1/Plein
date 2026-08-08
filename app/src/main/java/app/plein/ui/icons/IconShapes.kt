package app.plein.ui.icons

import android.graphics.Matrix
import androidx.annotation.StringRes
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
import app.plein.R

/**
 * Форма значка.
 *
 * Считается формулой через RoundedPolygon, а не рисуется руками: те же
 * фигуры, что в макете, и любая из них умеет перетекать в другую через Morph.
 */
enum class IconShape(@StringRes val titleRes: Int, private val builder: () -> RoundedPolygon) {

    Default(R.string.shape_default, { RoundedPolygon.rectangle(1f, 1f, CornerRounding(0.34f, 0.6f)) }),
    Circle(R.string.shape_circle, { RoundedPolygon.circle(numVertices = 12) }),
    Square(R.string.shape_square, { RoundedPolygon.rectangle(1f, 1f, CornerRounding(0.16f, 0.4f)) }),
    Clover(R.string.shape_clover, { RoundedPolygon.star(4, 1f, 0.62f, CornerRounding(0.55f), CornerRounding(0.35f)) }),
    Cookie6(R.string.shape_cookie6, { RoundedPolygon.star(6, 1f, 0.82f, CornerRounding(1f), CornerRounding(1f)) }),
    Cookie9(R.string.shape_cookie9, { RoundedPolygon.star(9, 1f, 0.86f, CornerRounding(1f), CornerRounding(1f)) }),
    Sunny(R.string.shape_sun, { RoundedPolygon.star(12, 1f, 0.9f, CornerRounding(1f), CornerRounding(1f)) }),
    Burst(R.string.shape_burst, { RoundedPolygon.star(8, 1f, 0.62f, CornerRounding(0.12f), CornerRounding(0.12f)) }),
    Flower(R.string.shape_flower, { RoundedPolygon.star(8, 1f, 0.68f, CornerRounding(0.9f), CornerRounding(0.45f)) }),
    Gem(R.string.shape_gem, { RoundedPolygon(numVertices = 8, rounding = CornerRounding(0.22f)) }),
    Pentagon(R.string.shape_pentagon, { RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.28f)) }),
    Diamond(R.string.shape_diamond, { RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.22f)) }),
    Triangle(R.string.shape_triangle, { RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.32f)).rotate(-90f) }),
    Pill(R.string.shape_pill, { RoundedPolygon.pill(width = 1f, height = 0.72f) });

    private val polygon: RoundedPolygon by lazy { builder().normalized() }
    private val shapeCache: Shape by lazy { PolygonShape(polygon) }

    fun shape(): Shape = if (this == Circle) CircleShape else shapeCache

    /**
     * Контур формы под размер значка.
     *
     * Нужен, чтобы обрезать иконку один раз при отрисовке в битмап: клип
     * произвольным путём на каждом кадре ронял прокрутку до пары кадров в секунду.
     */
    fun path(sizePx: Int): android.graphics.Path {
        val path = polygon.toPath()
        path.transform(Matrix().apply { setScale(sizePx.toFloat(), sizePx.toFloat()) })
        return path
    }

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
