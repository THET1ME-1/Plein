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
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Настройки",
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
                                    text = "Сделать лаунчером по умолчанию",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "Кнопка «Домой» будет открывать Plein",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                )
                            }
                        }
                    }
                }
            }

            item { SectionLabel("Форма значков") }
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

            item { SectionLabel("Сетка") }
            item {
                SettingRow(title = "Столбцы", subtitle = "Сколько значков в ряд") {
                    Segments(
                        values = listOf(3, 4, 5, 6),
                        selected = prefs.columns,
                        label = { it.toString() },
                        onSelect = { prefs.updateColumns(it) },
                    )
                }
            }
            item {
                SettingRow(title = "Названия приложений", subtitle = "Подписи под значками") {
                    Switch(
                        checked = prefs.showLabels,
                        onCheckedChange = { prefs.updateShowLabels(it) },
                    )
                }
            }
            item {
                SettingRow(title = "Тема телефона", subtitle = "Следовать системе") {
                    Switch(
                        checked = prefs.followSystemTheme,
                        onCheckedChange = { prefs.updateFollowSystemTheme(it) },
                    )
                }
            }
            if (!prefs.followSystemTheme) {
                item {
                    SettingRow(title = "Тёмная тема", subtitle = "Постоянно") {
                        Switch(
                            checked = prefs.darkTheme,
                            onCheckedChange = { prefs.updateDarkTheme(it) },
                        )
                    }
                }
            }

            item { SectionLabel("Папки") }
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
                            text = "Новая папка",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 14.dp),
                        )
                    }
                }
            }
        }
    }

    if (creating || renamingId != null) {
        val editingId = renamingId
        AlertDialog(
            onDismissRequest = { creating = false; renamingId = null },
            title = { Text(if (editingId == null) "Новая папка" else "Имя папки") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingId == null) onCreateFolder(draft) else onRenameFolder(editingId, draft)
                    creating = false
                    renamingId = null
                }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { creating = false; renamingId = null }) { Text("Отмена") }
            },
            shape = RoundedCornerShape(28.dp),
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
                        text = folder.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (folder.isAll) "Все установленные" else "${folder.appKeys.size} приложений",
                        fontFamily = MonoFont,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconAction(Icons.Rounded.ArrowUpward, "Выше") { onMove(index, index - 1) }
                IconAction(Icons.Rounded.ArrowDownward, "Ниже") { onMove(index, index + 1) }
                if (!folder.isAll) {
                    IconAction(Icons.Rounded.DriveFileRenameOutline, "Переименовать") {
                        onRename(folder.id, folder.title)
                    }
                    IconAction(Icons.Rounded.Delete, "Удалить", danger = true) { onDelete(folder.id) }
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
