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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.ui.home.AppIcon
import app.plein.ui.icons.IconShape
import app.plein.ui.theme.MonoFont

/**
 * Выбор приложения.
 *
 * Со значками и пакетом под именем: у половины телефона названия похожи, а
 * «Погода» стоит и у прошивки, и у стороннего приложения. Список ростом в
 * половину экрана, поиск сверху.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    title: String,
    apps: List<AppEntry>,
    repository: AppRepository,
    iconShape: IconShape,
    iconPack: String,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val unique = remember(apps) { apps.distinctBy { it.component.packageName } }
    val shown = remember(query, unique) {
        if (query.isBlank()) unique
        else unique.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.component.packageName.contains(query, ignoreCase = true)
        }
    }
    val listHeight = (LocalConfiguration.current.screenHeightDp * 0.52f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                text = title,
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
                modifier = Modifier.height(listHeight),
            ) {
                item {
                    PickerRow(
                        title = stringResource(R.string.weather_app_none),
                        subtitle = null,
                        selected = selected.isEmpty(),
                        onClick = { onPick(""); onDismiss() },
                        icon = {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.DoNotDisturbOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                }
                items(shown, key = { it.component.packageName }) { entry ->
                    PickerRow(
                        title = entry.title,
                        subtitle = entry.component.packageName,
                        selected = entry.component.packageName == selected,
                        onClick = { onPick(entry.component.packageName); onDismiss() },
                        icon = {
                            AppIcon(
                                entry = entry,
                                repository = repository,
                                size = 40.dp,
                                iconShape = iconShape,
                                iconPack = iconPack,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(22.dp))
            // Выбранное держится заливкой: обводок в лаунчере нет нигде.
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        icon()
        Column(
            Modifier
                .padding(start = 14.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontFamily = MonoFont,
                    fontSize = 10.sp,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
