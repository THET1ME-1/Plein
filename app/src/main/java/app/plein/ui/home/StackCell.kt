package app.plein.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.MonoMode
import app.plein.ui.icons.IconShape
import app.plein.ui.rememberHaptics

/**
 * Стопка редких приложений.
 *
 * Закрытая выглядит как один значок с уголками соседей, открытая раскрывается
 * веером. Веер, а не список: четыре штуки видно разом, и рука дотягивается до
 * любого, не отрывая пальца от экрана.
 */
@Composable
fun StackCell(
    apps: List<AppEntry>,
    repository: AppRepository,
    iconSize: Dp,
    iconShape: IconShape,
    iconPack: String,
    monoMode: MonoMode,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpen: (AppEntry) -> Unit,
) {
    val haptics = rememberHaptics()
    val spread by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "stack",
    )

    Box(
        Modifier
            .fillMaxSize()
            .clickable {
                haptics.tick()
                onToggle()
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        // Закрытая стопка: значки лежат друг на друге со сдвигом, как колода.
        apps.take(4).forEachIndexed { index, entry ->
            val step = index.toFloat()
            val shift = with(androidx.compose.ui.platform.LocalDensity.current) {
                (iconSize.toPx() * 0.62f)
            }
            Box(
                Modifier
                    .graphicsLayer {
                        val angle = (step - 1.5f) * 18f
                        translationX = spread * shift * (step - 1.5f)
                        translationY = (1f - spread) * step * 3f + spread * kotlin.math.abs(step - 1.5f) * 6f
                        rotationZ = (1f - spread) * angle * 0.15f
                        scaleX = 1f - (1f - spread) * step * 0.06f
                        scaleY = scaleX
                    }
                    .size(iconSize)
                    .clip(RoundedCornerShape(iconSize / 4))
                    .clickable(enabled = expanded) { onOpen(entry) },
            ) {
                AppIcon(
                    entry = entry,
                    repository = repository,
                    size = iconSize,
                    iconShape = iconShape,
                    iconPack = iconPack,
                    monoMode = monoMode,
                )
            }
        }

        if (!expanded) {
            // Точка-подсказка: значок с точкой это стопка, а не приложение.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
