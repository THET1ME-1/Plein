package app.plein.ui.home

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.random.Random

/**
 * Появление кадра: случайная фигура растекается в прямоугольник.
 *
 * Форма считается заново на каждую смену, а не берётся из набора: смысл в том,
 * что два переключения подряд выглядят по-разному.
 */
object MorphReveal {

    fun randomMorph(random: Random = Random.Default): Morph {
        val vertices = random.nextInt(3, 13)
        val innerRatio = 0.35f + random.nextFloat() * 0.5f
        val rounding = CornerRounding(random.nextFloat() * 0.9f)
        val innerRounding = CornerRounding(random.nextFloat() * 0.9f)

        val start = RoundedPolygon.star(
            numVerticesPerRadius = vertices,
            radius = 1f,
            innerRadius = innerRatio,
            rounding = rounding,
            innerRounding = innerRounding,
        ).normalized()

        val end = RoundedPolygon.rectangle(
            width = 2f,
            height = 2f,
            rounding = CornerRounding(0f),
        ).normalized()

        return Morph(start, end)
    }
}

/** Кадр обрезается промежуточной формой морфа: 0 — фигура, 1 — весь прямоугольник. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress.coerceIn(0f, 1f)).asComposePath()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}
