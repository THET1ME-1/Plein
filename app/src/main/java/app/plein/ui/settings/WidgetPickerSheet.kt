package app.plein.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import app.plein.R
import app.plein.data.Widgets
import app.plein.ui.theme.MonoFont

/**
 * Выбор виджета из установленных приложений.
 *
 * Список даёт система, и он длинный: у одного приложения бывает пять виджетов.
 * Показываем предпросмотр, название и требуемый размер в клетках — по нему
 * сразу видно, влезет ли виджет в сетку.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    widgets: Widgets,
    columns: Int,
    rowHeightDp: Int,
    onPick: (Widgets.Provider, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val cellWidthDp = (screenWidth - 40) / columns

    val all = remember { widgets.providers() }
    val shown = remember(query, all) {
        if (query.isBlank()) all
        else all.filter { it.label.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                text = stringResource(R.string.add_widget),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, bottom = 12.dp),
            )
            PlainSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.search_apps),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                modifier = Modifier.height((LocalConfiguration.current.screenHeightDp * 0.55f).dp),
            ) {
                items(shown, key = { it.info.provider.flattenToString() + it.label }) { provider ->
                    val (width, height) = widgets.cellsFor(provider.info, cellWidthDp, rowHeightDp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onPick(provider, width, height) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            provider.icon?.let { drawable ->
                                val bitmap = remember(drawable) {
                                    runCatching { drawable.toBitmap(72, 72) }.getOrNull()
                                }
                                bitmap?.let {
                                    androidx.compose.foundation.Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp),
                                    )
                                }
                            }
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 14.dp)
                        ) {
                            Text(
                                text = provider.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "$width × $height",
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
