package app.plein.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.ui.theme.MonoFont

/**
 * Плитки лаунчера.
 *
 * Свои, а не системные виджеты: рисуются в наших токенах, знают форму и цвет
 * темы, ничего не тянут через RemoteViews и не приносят чужой отрисовки в нашу
 * прокрутку. Системные виджеты придут отдельным слоем, эти останутся первыми.
 */
object Tiles {

    const val CLOCK = "clock"
    const val WEATHER = "weather"
    const val BATTERY = "battery"
    const val CALENDAR = "calendar"
    const val NOTE = "note"

    /** Размер по умолчанию в клетках: ширина к высоте. */
    fun sizeOf(kind: String): Pair<Int, Int> = when (kind) {
        CLOCK -> 2 to 2
        WEATHER -> 2 to 2
        BATTERY -> 2 to 1
        CALENDAR -> 4 to 1
        NOTE -> 4 to 2
        else -> 2 to 2
    }

    val all = listOf(CLOCK, WEATHER, BATTERY, CALENDAR, NOTE)
}

/** Общая оболочка плитки: заливка, форма, отступ от соседей. */
@Composable
fun TileSurface(
    modifier: Modifier = Modifier,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
    ) { content() }
}

@Composable
fun ClockTile(time: String, date: String, onClick: () -> Unit) {
    TileSurface(
        container = MaterialTheme.colorScheme.primaryContainer,
        onClick = onClick,
    ) {
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
            )
            Text(
                text = date,
                fontFamily = MonoFont,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun WeatherTile(temperature: String?, code: Int, place: String, onClick: () -> Unit) {
    TileSurface(onClick = onClick) {
        Column(Modifier.fillMaxSize()) {
            Icon(
                weatherIcon(code),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = temperature ?: "—",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = place,
                        fontFamily = MonoFont,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryTile(percent: Int, charging: Boolean) {
    TileSurface {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (charging) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.BatteryFull,
                    contentDescription = null,
                    tint = if (charging) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
fun CalendarTile(title: String, time: String, onClick: () -> Unit) {
    TileSurface(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            Text(
                text = time,
                fontFamily = MonoFont,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun NoteTile(text: String, onClick: () -> Unit) {
    TileSurface(
        container = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxSize()) {
            Icon(
                Icons.Rounded.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
