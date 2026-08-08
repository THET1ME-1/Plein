package app.plein.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Кирпичи экрана настроек.
 *
 * Группа это единая карточка: у первой строки скруглены верхние углы,
 * у последней нижние, между строками тонкая линия. Иконка живёт в круглом
 * цветном чипе, а не висит голой.
 */

private val GroupCorner = 28.dp
private val RowCorner = 6.dp

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(bottom = 18.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 14.dp),
            content = content,
        )
    }
}

/** Положение строки в группе: от него зависят скругления. */
enum class RowPlace { Single, First, Middle, Last }

fun RowPlace.shape(): RoundedCornerShape = when (this) {
    RowPlace.Single -> RoundedCornerShape(GroupCorner)
    RowPlace.First -> RoundedCornerShape(topStart = GroupCorner, topEnd = GroupCorner, bottomStart = RowCorner, bottomEnd = RowCorner)
    RowPlace.Middle -> RoundedCornerShape(RowCorner)
    RowPlace.Last -> RoundedCornerShape(topStart = RowCorner, topEnd = RowCorner, bottomStart = GroupCorner, bottomEnd = GroupCorner)
}

/** Круглый цветной чип под иконку. */
@Composable
fun IconChip(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    size: androidx.compose.ui.unit.Dp = 46.dp,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    place: RowPlace = RowPlace.Single,
    chipTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    chipBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = place.shape(),
        modifier = Modifier
            .fillMaxWidth()
            .clip(place.shape())
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            IconChip(icon = icon, tint = chipTint, background = chipBackground)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            trailing?.let {
                Spacer(Modifier.width(12.dp))
                it()
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    place: RowPlace = RowPlace.Single,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        place = place,
        onClick = { onCheckedChange(!checked) },
        trailing = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

/** Панель внутри группы: заголовок и произвольное содержимое во всю ширину. */
@Composable
fun SettingsPanel(
    title: String? = null,
    place: RowPlace = RowPlace.Single,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = place.shape(),
        modifier = Modifier
            .fillMaxWidth()
            .clip(place.shape()),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            content()
        }
    }
}

@Composable
fun GroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

/** Сегменты одной пилюлей: активная заливается, соседи остаются тихими. */
@Composable
fun <T> SegmentedPill(
    values: List<T>,
    selected: T,
    modifier: Modifier = Modifier,
    content: @Composable (value: T, active: Boolean) -> Unit,
    onSelect: (T) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = CircleShape,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight(),
        ) {
            values.forEach { value ->
                val active = value == selected
                Box(
                    Modifier
                        .weight(1f)
                        // Высота обязательна: без неё ячейка сжимается до размера
                        // содержимого, заливка уезжает полоской вверх.
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    content(value, active)
                }
            }
        }
    }
}

typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope
