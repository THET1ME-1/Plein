package app.plein.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.SeedPresets
import app.plein.ui.theme.ThemeMode
import app.plein.ui.theme.Vibrancy
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * Оформление: палитра кружками, режим темы значками, сочность словами.
 * Режим показан солнцем, луной, телефоном и часами — подписи тут лишние.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearancePanel(
    themeMode: ThemeMode,
    amoled: Boolean,
    vibrancy: Vibrancy,
    seedColor: Int,
    seedFromPhoto: Boolean,
    dark: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onVibrancy: (Vibrancy) -> Unit,
    onSeedColor: (Int) -> Unit,
    onPickCustomColor: () -> Unit,
) {
    SettingsPanel(place = RowPlace.First) {
        if (!seedFromPhoto) {
            Text(
                text = stringResource(R.string.accent_color),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 20.dp),
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
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(onClick = onPickCustomColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Colorize,
                        contentDescription = stringResource(R.string.custom_color),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.theme_mode),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        SegmentedPill(
            values = ThemeMode.entries,
            selected = themeMode,
            onSelect = onThemeMode,
            content = { mode, active ->
                Icon(
                    imageVector = mode.icon(),
                    contentDescription = stringResource(mode.titleRes),
                    tint = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            },
        )

        Text(
            text = stringResource(R.string.saturation),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        )
        SegmentedPill(
            values = Vibrancy.entries,
            selected = vibrancy,
            onSelect = onVibrancy,
            content = { value, active ->
                Text(
                    text = stringResource(value.titleRes),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.Light -> Icons.Rounded.LightMode
    ThemeMode.Dark -> Icons.Rounded.Bedtime
    ThemeMode.System -> Icons.Rounded.PhoneAndroid
    ThemeMode.AutoTime -> Icons.Rounded.Schedule
}

/**
 * Кружок пресета: четыре тона будущей схемы и галочка на выбранном.
 * Видно, во что превратится тема, ещё до нажатия.
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
        style = when (vibrancy) {
            Vibrancy.Soft -> PaletteStyle.TonalSpot
            Vibrancy.Vibrant -> PaletteStyle.Vibrant
            Vibrancy.Fidelity -> PaletteStyle.Fidelity
        },
    )
    val tones = listOf(scheme.primaryContainer, scheme.primary, scheme.secondary, scheme.tertiary)
    val ring = MaterialTheme.colorScheme.onSurface

    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(if (selected) 44.dp else 52.dp)) {
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
            Canvas(Modifier.size(52.dp)) {
                drawCircle(color = ring, radius = size.minDimension / 2f - 2f, style = Stroke(width = 5f))
            }
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
