package app.plein.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.data.FolderConfig
import app.plein.data.Prefs
import app.plein.ui.icons.IconShape
import androidx.compose.ui.res.stringResource
import app.plein.R
import app.plein.ui.theme.MonoFont

/**
 * Настройки одним экраном: форма значков, сетка, папки и роль лаунчера.
 * Порядок разделов повторяет макет.
 */
@Composable
fun SettingsScreen(
    prefs: Prefs,
    folders: List<FolderConfig>,
    isDefaultLauncher: Boolean,
    dark: Boolean,
    onClose: () -> Unit,
    onMakeDefault: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onMoveFolder: (Int, Int) -> Unit,
) {
    var allShapes by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            if (!isDefaultLauncher) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable(onClick = onMakeDefault),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Column(Modifier.padding(start = 14.dp)) {
                                Text(
                                    text = stringResource(R.string.make_default),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = stringResource(R.string.make_default_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                )
                            }
                        }
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.icon_shape)) }
            item {
                val shapes = if (allShapes) IconShape.entries.toList() else IconShape.featured
                Column {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (allShapes) 380.dp else 100.dp),
                    ) {
                        items(shapes, key = { it.name }) { option ->
                            ShapeCell(
                                option = option,
                                selected = prefs.iconShape == option,
                                onClick = { prefs.updateIconShape(option) },
                            )
                        }
                    }
                    TextButton(
                        onClick = { allShapes = !allShapes },
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Text(if (allShapes) "Свернуть" else "Больше форм")
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.grid)) }
            item {
                SettingRow(title = stringResource(R.string.columns), subtitle = stringResource(R.string.columns_hint)) {
                    Segments(
                        values = listOf(3, 4, 5, 6),
                        selected = prefs.columns,
                        label = { it.toString() },
                        onSelect = { prefs.updateColumns(it) },
                    )
                }
            }
            item {
                SettingRow(title = stringResource(R.string.labels), subtitle = stringResource(R.string.labels_hint)) {
                    Switch(checked = prefs.showLabels, onCheckedChange = { prefs.updateShowLabels(it) })
                }
            }

            item { SectionLabel(stringResource(R.string.appearance)) }
            item {
                AppearanceSection(
                    themeMode = prefs.themeMode,
                    amoled = prefs.amoled,
                    dynamicColor = prefs.dynamicColor,
                    vibrancy = prefs.vibrancy,
                    seedColor = prefs.seedColor,
                    seedFromPhoto = prefs.seedFromPhoto,
                    dark = dark,
                    onThemeMode = { prefs.updateThemeMode(it) },
                    onAmoled = { prefs.updateAmoled(it) },
                    onDynamicColor = { prefs.updateDynamicColor(it) },
                    onVibrancy = { prefs.updateVibrancy(it) },
                    onSeedColor = { prefs.updateSeedColor(it) },
                    onSeedFromPhoto = { prefs.updateSeedFromPhoto(it) },
                    onPickCustomColor = { picking = true },
                )
            }

            item { SectionLabel(stringResource(R.string.folders)) }
            itemsIndexedFolders(
                folders = folders,
                onRename = { id, title -> renamingId = id; draft = title },
                onDelete = onDeleteFolder,
                onMove = onMoveFolder,
            )
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { creating = true; draft = "" },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.new_folder),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 14.dp),
                        )
                    }
                }
            }
        }
    }

    if (picking) {
        ColorPickerSheet(
            onPick = { prefs.updateSeedColor(it) },
            onDismiss = { picking = false },
        )
    }

    if (creating || renamingId != null) {
        val editingId = renamingId
        NameSheet(
            title = if (editingId == null) stringResource(R.string.new_folder) else stringResource(R.string.folder_name),
            value = draft,
            onValueChange = { draft = it },
            onConfirm = {
                if (editingId == null) onCreateFolder(draft) else onRenameFolder(editingId, draft)
                creating = false
                renamingId = null
            },
            onDismiss = { creating = false; renamingId = null },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedFolders(
    folders: List<FolderConfig>,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    items(folders.size, key = { folders[it].id }) { index ->
        val folder = folders[index]
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (folder.isAll) stringResource(R.string.all_apps) else folder.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (folder.isAll) "Все установленные" else stringResource(R.string.apps_count, folder.appKeys.size),
                        fontFamily = MonoFont,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconAction(Icons.Rounded.ArrowUpward, stringResource(R.string.move_up)) { onMove(index, index - 1) }
                IconAction(Icons.Rounded.ArrowDownward, stringResource(R.string.move_down)) { onMove(index, index + 1) }
                if (!folder.isAll) {
                    IconAction(Icons.Rounded.DriveFileRenameOutline, stringResource(R.string.rename)) {
                        onRename(folder.id, folder.title)
                    }
                    IconAction(Icons.Rounded.Delete, stringResource(R.string.delete), danger = true) { onDelete(folder.id) }
                }
            }
        }
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun ShapeCell(option: IconShape, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(option.shape())
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                )
        )
        Text(
            text = option.title,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = MonoFont,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
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
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun <T> Segments(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        values.forEach { value ->
            val active = value == selected
            Surface(
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelect(value) },
            ) {
                Text(
                    text = label(value),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}


/** Ввод имени: лист снизу, поле и две кнопки-пилюли. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NameSheet(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text(stringResource(R.string.cancel)) }
                androidx.compose.material3.Button(
                    onClick = onConfirm,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text(stringResource(R.string.done)) }
            }
        }
    }
}
