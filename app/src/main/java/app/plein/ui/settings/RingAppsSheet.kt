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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.RingFolders
import app.plein.ui.home.AppIcon
import app.plein.ui.icons.IconShape
import app.plein.ui.theme.MonoFont

/**
 * Состав круговой папки.
 *
 * Выбор здесь множественный и с порядком: номер рядом со значком говорит,
 * каким по счёту приложение встанет в кольцо, поэтому видно, какие шесть
 * окажутся снаружи, а какие уедут за середину.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingAppsSheet(
    title: String,
    apps: List<AppEntry>,
    repository: AppRepository,
    iconShape: IconShape,
    iconPack: String,
    selected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val picked = remember(selected) { mutableStateListOf<String>().apply { addAll(selected) } }

    val shown = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter {
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
                contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp),
                modifier = Modifier.height(listHeight),
            ) {
                items(shown, key = { it.key }) { entry ->
                    val place = picked.indexOf(entry.key)
                    RingRow(
                        entry = entry,
                        repository = repository,
                        iconShape = iconShape,
                        iconPack = iconPack,
                        place = place,
                        onClick = {
                            if (place >= 0) picked.removeAt(place) else picked.add(entry.key)
                        },
                    )
                }
            }
            Button(
                onClick = { onConfirm(picked.toList()) },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(stringResource(R.string.ring_done))
            }
        }
    }
}

@Composable
private fun RingRow(
    entry: AppEntry,
    repository: AppRepository,
    iconShape: IconShape,
    iconPack: String,
    place: Int,
    onClick: () -> Unit,
) {
    val chosen = place >= 0
    // Первые шесть стоят кольцом, дальше — за серединой: помечаем это прямо
    // в строке, иначе состав приходится держать в голове.
    val inRing = chosen && place < RingFolders.RING

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (chosen) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        AppIcon(
            entry = entry,
            repository = repository,
            size = 40.dp,
            iconShape = iconShape,
            iconPack = iconPack,
        )
        Column(
            Modifier
                .padding(start = 14.dp)
                .weight(1f)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = if (chosen) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.component.packageName,
                fontFamily = MonoFont,
                fontSize = 10.sp,
                color = if (chosen) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (chosen) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (inRing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (inRing) {
                    Text(
                        text = "${place + 1}",
                        fontFamily = MonoFont,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}
