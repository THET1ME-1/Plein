package app.plein.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.plein.data.Widgets

/**
 * Виджет приложения внутри нашей сетки.
 *
 * Чужая отрисовка живёт во View, поэтому оборачиваем её в AndroidView и
 * скругляем клипом — иначе прямоугольник спорит с формой всего остального.
 * Размер сообщаем виджету явно: без этого он рисует по своему усмотрению и
 * либо жмётся в угол, либо вылезает.
 */
@Composable
fun WidgetView(
    widgets: Widgets,
    widgetId: Int,
    widthCells: Int,
    heightCells: Int,
    columns: Int,
    rowHeightDp: Int,
) {
    val info = remember(widgetId) { widgets.infoOf(widgetId) }
    val screenWidth = LocalConfiguration.current.screenWidthDp

    if (info == null) {
        // Приложение удалили или виджет отвязали: место держим, но честно
        // говорим, что показывать нечего.
        Box(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "—",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val cellWidthDp = (screenWidth - 40) / columns
    val widthDp = cellWidthDp * widthCells - 8
    val heightDp = rowHeightDp * heightCells - 8

    Box(
        Modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        AndroidView(
            factory = { widgets.createView(widgetId, info) },
            update = { view -> widgets.applySize(view, widthDp, heightDp) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
