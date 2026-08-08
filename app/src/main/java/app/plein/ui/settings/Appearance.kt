package app.plein.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.SeedPresets
import app.plein.ui.theme.ThemeMode
import app.plein.ui.theme.Vibrancy
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * Блок «Внешний вид» по образцу Wickly: режимы темы сегментами, тумблеры,
 * палитра пресетов кружками из четырёх тонов и свой цвет пипеткой.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSection(
    themeMode: ThemeMode,
    amoled: Boolean,
    dynamicColor: Boolean,
    vibrancy: Vibrancy,
    seedColor: Int,
    seedFromPhoto: Boolean,
    dark: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onAmoled: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onVibrancy: (Vibrancy) -> Unit,
    onSeedColor: (Int) -> Unit,
    onSeedFromPhoto: (Boolean) -> Unit,
    onPickCustomColor: () -> Unit,
) {
    Column {
        SettingCard {
            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            PillSegments(
                values = ThemeMode.entries,
                selected = themeMode,
                label = { stringResource(it.titleRes) },
                onSelect = onThemeMode,
            )
        }

        if (themeMode != ThemeMode.Light) {
            ToggleRow(
                title = stringResource(R.string.amoled),
                subtitle = stringResource(R.string.amoled_hint),
                checked = amoled,
                onCheckedChange = onAmoled,
            )
        }

        ToggleRow(
            title = stringResource(R.string.material_you),
            subtitle = stringResource(R.string.material_you_hint),
            checked = dynamicColor,
            onCheckedChange = onDynamicColor,
        )

        ToggleRow(
            title = stringResource(R.string.color_from_photo),
            subtitle = stringResource(R.string.color_from_photo_hint),
            checked = seedFromPhoto,
            onCheckedChange = onSeedFromPhoto,
        )

        if (!dynamicColor && !seedFromPhoto) {
            SettingCard {
                Text(
                    text = stringResource(R.string.accent_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SeedPresets.forEach { preset ->
                        SeedSwatch(
                            seed = preset,
                            dark = dark,
                            vibrancy = vibrancy,
                            selected = preset == seedColor,
                            onClick = { onSeedColor(preset) },
                        )
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable(onClick = onPickCustomColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Colorize,
                            contentDescription = stringResource(R.string.custom_color),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            SettingCard {
                Text(
                    text = stringResource(R.string.saturation),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                PillSegments(
                    values = Vibrancy.entries,
                    selected = vibrancy,
                    label = { stringResource(it.titleRes) },
                    onSelect = onVibrancy,
                )
            }
        }
    }
}

/**
 * Кружок пресета: четыре тона схемы, собранной из этого seed.
 * По нему видно, во что превратится тема, ещё до нажатия.
 */
@Composable
fun SeedSwatch(
    seed: Int,
    dark: Boolean,
    vibrancy: Vibrancy,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = rememberDynamicColorScheme(
        seedColor = Color(seed),
        isDark = dark,
        style = if (vibrancy == Vibrancy.Vibrant) PaletteStyle.Vibrant else PaletteStyle.Fidelity,
    )
    val tones = listOf(scheme.primary, scheme.primaryContainer, scheme.tertiary, scheme.secondary)
    val ring = MaterialTheme.colorScheme.onSurface

    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(if (selected) 36.dp else 44.dp)) {
            tones.forEachIndexed { index, tone ->
                drawArc(
                    color = tone,
                    startAngle = index * 90f - 90f,
                    sweepAngle = 90f,
                    useCenter = true,
                )
            }
        }
        if (selected) {
            Canvas(Modifier.size(44.dp)) {
                drawCircle(color = ring, radius = size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
            }
        }
    }
}

@Composable
fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), content = content)
    }
}

/** Кнопки только пилюлей: это фирменная черта ДНК. */
@Composable
fun <T> PillSegments(
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value ->
            val active = value == selected
            Surface(
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(value) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label(value),
                        fontSize = 12.5.sp,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}


@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onCheckedChange(!checked) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
