package app.plein.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.AppShortcut
import app.plein.data.FolderConfig
import app.plein.data.displayTitle
import app.plein.ui.home.AppIcon
import app.plein.ui.settings.PlainField
import app.plein.ui.theme.MonoFont

/**
 * Меню долгого нажатия: всё про одно приложение в одном листе.
 * Виджетов здесь нет: домашний экран занят кадром и сеткой.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMenuSheet(
    entry: AppEntry,
    repository: AppRepository,
    folders: List<FolderConfig>,
    memberOf: Set<String>,
    iconShape: app.plein.ui.icons.IconShape,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onStartReorder: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renaming by remember { mutableStateOf(false) }
    var draft by remember(entry.key) { mutableStateOf(entry.title) }

    // Быстрые действия спрашиваем у системы при открытии листа: держать их
    // заранее бессмысленно, приложение меняет их когда захочет.
    val iconPx = with(LocalDensity.current) { 22.dp.roundToPx() }
    val shortcuts by produceState(initialValue = emptyList<AppShortcut>(), entry.key) {
        value = repository.shortcuts(entry, iconPx)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            ) {
                AppIcon(entry = entry, repository = repository, size = 52.dp, iconShape = iconShape)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = entry.component.packageName,
                        fontFamily = MonoFont,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }

            if (renaming) {
                PlainField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = stringResource(R.string.app_custom_name),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    TextButton(onClick = { renaming = false; draft = entry.title }) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { onRename(draft); renaming = false }) { Text(stringResource(R.string.save)) }
                }
            } else {
                if (shortcuts.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.shortcuts))
                    shortcuts.forEach { shortcut ->
                        MenuRow(
                            title = shortcut.label,
                            onClick = {
                                repository.startShortcut(shortcut)
                                onDismiss()
                            },
                            leading = {
                                val bitmap = shortcut.icon
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.OpenInNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                        )
                    }
                }

                SectionLabel(stringResource(R.string.app_section))
                MenuRow(Icons.Rounded.SwapHoriz, stringResource(R.string.reorder), stringResource(R.string.reorder_hint), onClick = onStartReorder)
                MenuRow(Icons.Rounded.DriveFileRenameOutline, stringResource(R.string.app_custom_name), stringResource(R.string.app_custom_name_hint), onClick = { renaming = true })
                MenuRow(Icons.Rounded.Info, stringResource(R.string.app_info), stringResource(R.string.app_info_hint), onClick = {
                    repository.openAppInfo(entry)
                    onDismiss()
                })
                if (entry.component.packageName != "app.plein") {
                    MenuRow(
                        icon = Icons.Rounded.Delete,
                        title = stringResource(R.string.uninstall),
                        subtitle = stringResource(R.string.uninstall_hint),
                        danger = true,
                        onClick = {
                            repository.uninstall(entry)
                            onDismiss()
                        },
                    )
                }
            }

            if (folders.any { !it.isAll }) {
                SectionLabel(stringResource(R.string.folders))
                folders.filter { !it.isAll }.forEach { folder ->
                    val inside = folder.id in memberOf
                    MenuRow(
                        icon = if (inside) Icons.Rounded.Check else Icons.Rounded.FolderOpen,
                        title = folder.displayTitle(),
                        subtitle = if (inside) stringResource(R.string.in_folder) else stringResource(R.string.add_to_folder),
                        highlighted = inside,
                        onClick = { onToggleFolder(folder.id) },
                    )
                }
            }
        }
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
        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 8.dp),
    )
}

/** Строка с системным значком: почти все пункты меню такие. */
@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    highlighted: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    MenuRow(
        title = title,
        subtitle = subtitle,
        highlighted = highlighted,
        danger = danger,
        onClick = onClick,
        leading = {
            Icon(
                icon,
                contentDescription = null,
                tint = when {
                    danger -> MaterialTheme.colorScheme.error
                    highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
        },
    )
}

/** Строка со своим значком: быстрые действия приходят с картинкой приложения. */
@Composable
private fun MenuRow(
    title: String,
    subtitle: String? = null,
    highlighted: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Surface(
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            leading()
            Column(Modifier.padding(start = 14.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                    color = when {
                        danger -> MaterialTheme.colorScheme.error
                        highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
