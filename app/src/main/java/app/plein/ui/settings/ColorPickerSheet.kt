package app.plein.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R

/**
 * Свой цвет: сетка оттенков по кругу и по светлоте.
 * Лист снизу и во всю ширину, как остальные попапы.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = remember { buildPalette() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 20.dp)) {
            Text(
                text = stringResource(R.string.custom_color),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, bottom = 14.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                items(colors) { argb ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .clickable {
                                onPick(argb)
                                onDismiss()
                            }
                    )
                }
            }
        }
    }
}

/** Восемь оттенков по кругу на пять ступеней светлоты: сорок живых цветов. */
private fun buildPalette(): List<Int> {
    val hues = List(8) { it * 45f }
    val levels = listOf(0.45f to 0.85f, 0.55f to 0.75f, 0.65f to 0.62f, 0.75f to 0.5f, 0.35f to 0.95f)
    return levels.flatMap { (saturation, value) ->
        hues.map { hue ->
            android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        }
    }
}
