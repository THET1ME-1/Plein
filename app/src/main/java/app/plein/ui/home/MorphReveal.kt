package app.plein.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import app.plein.ui.theme.Emphasized
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

/**
 * Знак загрузки кадра — та самая фигура, которой кадр потом растечётся.
 *
 * Пока тянут вниз, фигура растёт под пальцем и доворачивается. Дотянули —
 * она крутится и дышит, пока идёт запрос. Круглая крутилка тут выглядела
 * чужой заплаткой: у появления кадра своя пластика, знак должен быть из неё.
 */
@Composable
fun MorphSpinner(
    morph: Morph,
    size: Dp,
    color: Color,
    spinning: Boolean,
    turn: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "morph-spinner")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "spin",
    )
    val breath by infinite.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = Emphasized), RepeatMode.Reverse),
        label = "breath",
    )

    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationZ = if (spinning) spin else turn
                val scale = if (spinning) breath else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(MorphShape(morph, 0f))
            .background(color)
    )
}
