package app.plein.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.FolderConfig
import app.plein.data.displayTitle
import app.plein.data.Prefs
import app.plein.ui.icons.IconShape

/**
 * Настройки.
 *
 * Секция это единая карточка: у крайних строк скруглены внешние углы, между
 * строками почти прямой стык. Иконка сидит в круглом цветном чипе.
 */
@Composable
fun SettingsScreen(
    prefs: Prefs,
    repository: app.plein.data.AppRepository,
    folders: List<FolderConfig>,
    isDefaultLauncher: Boolean,
    dark: Boolean,
    versionName: String,
    onClose: () -> Unit,
    onMakeDefault: () -> Unit,
    onOpenSource: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onMoveFolder: (Int, Int) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    backdropFailure: String? = null,
    onPickPhotos: () -> Unit,
    onPickFolder: () -> Unit,
    onSetWallpaper: (app.plein.data.WallpaperTarget) -> Unit,
    selfUpdating: Boolean,
    updateState: String,
    onCheckUpdates: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    var pickingLanguage by remember { mutableStateOf(false) }
    var pickingWeatherApp by remember { mutableStateOf(false) }
    var pickingFontFor by remember { mutableStateOf<String?>(null) }
    var allShapes by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var choosingWallpaper by remember { mutableStateOf(false) }
    var pickingWeb by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val installed by repository.apps.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 40.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
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
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }

            if (!isDefaultLauncher) {
                item {
                    Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        SettingsRow(
                            icon = Icons.Rounded.Home,
                            title = stringResource(R.string.make_default),
                            subtitle = stringResource(R.string.make_default_hint),
                            chipTint = MaterialTheme.colorScheme.onPrimary,
                            chipBackground = MaterialTheme.colorScheme.primary,
                            onClick = onMakeDefault,
                        )
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.appearance)) {
                    AppearancePanel(
                        themeMode = prefs.themeMode,
                        amoled = prefs.amoled,
                        vibrancy = prefs.vibrancy,
                        seedColor = prefs.seedColor,
                        seedFromPhoto = prefs.seedFromPhoto,
                        dark = dark,
                        onThemeMode = { prefs.updateThemeMode(it) },
                        onVibrancy = { prefs.updateVibrancy(it) },
                        onSeedColor = { prefs.updateSeedColor(it) },
                        onPickCustomColor = { picking = true },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Image,
                        title = stringResource(R.string.color_from_photo),
                        subtitle = stringResource(R.string.color_from_photo_hint),
                        checked = prefs.seedFromPhoto,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateSeedFromPhoto(it) },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Wallpaper,
                        title = stringResource(R.string.material_you),
                        subtitle = stringResource(R.string.material_you_hint),
                        checked = prefs.dynamicColor,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateDynamicColor(it) },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Contrast,
                        title = stringResource(R.string.amoled),
                        subtitle = stringResource(R.string.amoled_hint),
                        checked = prefs.amoled,
                        place = RowPlace.Last,
                        onCheckedChange = { prefs.updateAmoled(it) },
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.screen)) {
                    SettingsPanel(title = stringResource(R.string.columns), place = RowPlace.First) {
                        SegmentedPill(
                            values = listOf(3, 4, 5, 6),
                            selected = prefs.columns,
                            onSelect = { prefs.updateColumns(it) },
                            content = { value, active ->
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                                    color = if (active) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                    SettingsPanel(title = stringResource(R.string.icon_shape), place = RowPlace.Middle) {
                        val shapes = if (allShapes) IconShape.entries.toList() else IconShape.featured
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            userScrollEnabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (allShapes) 470.dp else 122.dp),
                        ) {
                            items(shapes, key = { it.name }) { option ->
                                ShapeCell(
                                    option = option,
                                    selected = prefs.iconShape == option,
                                    onClick = { prefs.updateIconShape(option) },
                                )
                            }
                        }
                        Text(
                            text = stringResource(if (allShapes) R.string.collapse else R.string.more_shapes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .clip(CircleShape)
                                .clickable { allShapes = !allShapes }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                    SettingsPanel(title = stringResource(R.string.mono_icons), place = RowPlace.Middle) {
                        SegmentedPill(
                            values = listOf(
                                app.plein.data.MonoMode.Off,
                                app.plein.data.MonoMode.Declared,
                                app.plein.data.MonoMode.Always,
                            ),
                            selected = prefs.monoIcons,
                            onSelect = { prefs.updateMonoIcons(it) },
                            content = { value, active ->
                                Text(
                                    text = stringResource(
                                        when (value) {
                                            app.plein.data.MonoMode.Declared -> R.string.mono_declared
                                            app.plein.data.MonoMode.Always -> R.string.mono_always
                                            else -> R.string.mono_off
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                    color = if (active) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                        Text(
                            text = stringResource(R.string.mono_hint),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    SettingsToggleRow(
                        icon = Icons.Rounded.Label,
                        title = stringResource(R.string.labels),
                        subtitle = stringResource(R.string.labels_hint),
                        checked = prefs.showLabels,
                        place = RowPlace.Last,
                        onCheckedChange = { prefs.updateShowLabels(it) },
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.clock)) {
                    SettingsPanel(title = stringResource(R.string.clock_size), place = RowPlace.First) {
                        SegmentedPill(
                            values = listOf("s", "m", "l", "xl"),
                            selected = prefs.clockSize,
                            onSelect = { prefs.updateClockSize(it) },
                            content = { value, active ->
                                Text(
                                    text = value.uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                    color = if (active) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                    SettingsToggleRow(
                        icon = Icons.Rounded.Schedule,
                        title = "24",
                        subtitle = if (prefs.clockTwentyFour) "13:45" else "1:45 PM",
                        checked = prefs.clockTwentyFour,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateClockTwentyFour(it) },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.CalendarMonth,
                        title = stringResource(R.string.show_date),
                        checked = prefs.showDate,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateShowDate(it) },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Cloud,
                        title = stringResource(R.string.show_weather),
                        subtitle = stringResource(R.string.weather),
                        checked = prefs.showWeather,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateShowWeather(it) },
                    )
                    if (prefs.showWeather) {
                        SettingsPanel(title = stringResource(R.string.weather), place = RowPlace.Middle) {
                            SegmentedPill(
                                values = listOf("open-meteo", "met.no"),
                                selected = prefs.weatherProvider,
                                onSelect = { prefs.updateWeatherProvider(it) },
                                content = { value, active ->
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                        color = if (active) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Cloud,
                        title = stringResource(R.string.weather_app),
                        // Показываем название, а не пакет: «com.miui.weather2» в
                        // настройках читалось как ошибка.
                        subtitle = installed.firstOrNull { it.component.packageName == prefs.weatherApp }
                            ?.title
                            ?: stringResource(R.string.weather_app_hint),
                        place = RowPlace.Middle,
                        onClick = { pickingWeatherApp = true },
                    )
                    SettingsPanel(title = stringResource(R.string.page_indicator), place = RowPlace.Middle) {
                        SegmentedPill(
                            values = listOf("dots", "bar", "numbers", "none"),
                            selected = prefs.pageIndicator,
                            onSelect = { prefs.updatePageIndicator(it) },
                            content = { value, active ->
                                Text(
                                    text = stringResource(
                                        when (value) {
                                            "bar" -> R.string.indicator_bar
                                            "numbers" -> R.string.indicator_numbers
                                            "none" -> R.string.indicator_none
                                            else -> R.string.indicator_dots
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                    color = if (active) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                    SettingsRow(
                        icon = Icons.Rounded.FormatSize,
                        title = stringResource(R.string.clock),
                        subtitle = prefs.clockFont.ifEmpty { "Unbounded" },
                        place = RowPlace.Last,
                        onClick = { pickingFontFor = "clock" },
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.icon_pack)) {
                    val packs = remember { repository.installedIconPacks() }
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.system_icons),
                        place = if (packs.isEmpty()) RowPlace.Single else RowPlace.First,
                        chipTint = if (prefs.iconPack.isEmpty()) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        chipBackground = if (prefs.iconPack.isEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer,
                        onClick = { prefs.updateIconPack("") },
                    )
                    packs.forEachIndexed { index, pack ->
                        val active = pack.packageName == prefs.iconPack
                        SettingsRow(
                            icon = Icons.Rounded.Palette,
                            title = pack.label,
                            subtitle = pack.packageName,
                            place = if (index == packs.lastIndex) RowPlace.Last else RowPlace.Middle,
                            chipTint = if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            chipBackground = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer,
                            onClick = { prefs.updateIconPack(pack.packageName) },
                        )
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.interface_font)) {
                    SettingsRow(
                        icon = Icons.Rounded.TextFields,
                        title = stringResource(R.string.interface_font),
                        subtitle = prefs.interfaceFont.ifEmpty { "Onest · " + stringResource(R.string.font_search_hint) },
                        onClick = { pickingFontFor = "ui" },
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.folders)) {
                    folders.forEachIndexed { index, folder ->
                        val folderTitle = folder.displayTitle()
                        SettingsRow(
                            icon = Icons.Rounded.Folder,
                            title = folderTitle,
                            subtitle = if (folder.isAll) stringResource(R.string.all_installed)
                            else stringResource(R.string.apps_count, folder.appKeys.size),
                            place = if (index == 0) RowPlace.First else RowPlace.Middle,
                            trailing = {
                                Row {
                                    IconAction(Icons.Rounded.ArrowUpward, stringResource(R.string.move_up)) {
                                        onMoveFolder(index, index - 1)
                                    }
                                    IconAction(Icons.Rounded.ArrowDownward, stringResource(R.string.move_down)) {
                                        onMoveFolder(index, index + 1)
                                    }
                                    if (!folder.isAll) {
                                        IconAction(Icons.Rounded.DriveFileRenameOutline, stringResource(R.string.rename)) {
                                            renamingId = folder.id
                                            draft = folderTitle
                                        }
                                        IconAction(Icons.Rounded.Delete, stringResource(R.string.delete), danger = true) {
                                            onDeleteFolder(folder.id)
                                        }
                                    }
                                }
                            },
                        )
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Add,
                        title = stringResource(R.string.new_folder),
                        place = RowPlace.Last,
                        chipTint = MaterialTheme.colorScheme.onTertiaryContainer,
                        chipBackground = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = { creating = true; draft = "" },
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.language)) {
                    SettingsRow(
                        icon = Icons.Rounded.Search,
                        title = stringResource(R.string.web_provider),
                        subtitle = when (prefs.webProvider) {
                            "ddg" -> stringResource(R.string.web_ddg)
                            "bing" -> stringResource(R.string.web_bing)
                            "yandex" -> stringResource(R.string.web_yandex)
                            "startpage" -> stringResource(R.string.web_startpage)
                            else -> stringResource(R.string.web_google)
                        },
                        place = RowPlace.Middle,
                        onClick = { pickingWeb = true },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Language,
                        title = stringResource(R.string.language),
                        subtitle = app.plein.data.Language.titleOf(prefs.language),
                        onClick = { pickingLanguage = true },
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.backdrop)) {
                    SettingsPanel(title = stringResource(R.string.backdrop_source), place = RowPlace.First) {
                        SegmentedPill(
                            values = app.plein.data.BackdropOrigin.entries.toList(),
                            selected = prefs.backdropOrigin,
                            onSelect = { prefs.updateBackdropOrigin(it) },
                            content = { value, active ->
                                Text(
                                    text = stringResource(
                                        when (value) {
                                            app.plein.data.BackdropOrigin.Wikimedia -> R.string.backdrop_wikimedia
                                            app.plein.data.BackdropOrigin.Openverse -> R.string.backdrop_openverse
                                            app.plein.data.BackdropOrigin.Gallery -> R.string.backdrop_gallery
                                            app.plein.data.BackdropOrigin.Folder -> R.string.backdrop_folder
                                            else -> R.string.backdrop_wikimedia
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                    color = if (active) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                    backdropFailure?.let { reason ->
                        SettingsRow(
                            icon = Icons.Rounded.Cloud,
                            title = stringResource(R.string.backdrop_failed),
                            subtitle = reason,
                            place = RowPlace.Middle,
                            chipTint = MaterialTheme.colorScheme.onErrorContainer,
                            chipBackground = MaterialTheme.colorScheme.errorContainer,
                        )
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Image,
                        title = stringResource(R.string.pick_photos),
                        subtitle = stringResource(R.string.pick_photos_hint),
                        place = RowPlace.Middle,
                        onClick = onPickPhotos,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Folder,
                        title = stringResource(R.string.pick_folder),
                        subtitle = prefs.backdropFolder.ifEmpty { stringResource(R.string.pick_folder_hint) }
                            .substringAfterLast('/')
                            .ifEmpty { stringResource(R.string.pick_folder_hint) },
                        place = RowPlace.Middle,
                        onClick = onPickFolder,
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Schedule,
                        title = stringResource(R.string.backdrop_by_time),
                        subtitle = stringResource(R.string.backdrop_by_time_hint),
                        checked = prefs.backdropByTime,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateBackdropByTime(it) },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Cloud,
                        title = stringResource(R.string.backdrop_by_weather),
                        subtitle = stringResource(R.string.backdrop_by_weather_hint),
                        checked = prefs.backdropByWeather,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateBackdropByWeather(it) },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Wallpaper,
                        title = stringResource(R.string.backdrop_on_return),
                        subtitle = stringResource(R.string.backdrop_on_return_hint),
                        checked = prefs.backdropOnReturn,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateBackdropOnReturn(it) },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Link,
                        title = stringResource(R.string.wifi_only),
                        subtitle = stringResource(R.string.wifi_only_hint),
                        checked = prefs.backdropWifiOnly,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateBackdropWifiOnly(it) },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Wallpaper,
                        title = stringResource(R.string.set_wallpaper),
                        subtitle = stringResource(R.string.set_wallpaper_hint),
                        place = RowPlace.Last,
                        onClick = { choosingWallpaper = true },
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.backup)) {
                    SettingsRow(
                        icon = Icons.Rounded.Upload,
                        title = stringResource(R.string.backup_export),
                        subtitle = stringResource(R.string.backup_export_hint),
                        place = RowPlace.First,
                        onClick = onExportBackup,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Download,
                        title = stringResource(R.string.backup_import),
                        subtitle = stringResource(R.string.backup_import_hint),
                        place = RowPlace.Last,
                        onClick = onImportBackup,
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.gestures)) {
                    SettingsToggleRow(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.gesture_double_tap),
                        subtitle = stringResource(R.string.gesture_double_tap_hint),
                        checked = prefs.gestureDoubleTapLock,
                        place = RowPlace.First,
                        onCheckedChange = { prefs.updateGestureDoubleTapLock(it) },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.ArrowDownward,
                        title = stringResource(R.string.gesture_shade),
                        subtitle = stringResource(R.string.gesture_shade_hint),
                        checked = prefs.gestureShade,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateGestureShade(it) },
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Apps,
                        title = stringResource(R.string.gesture_pinch),
                        subtitle = stringResource(R.string.gesture_pinch_hint),
                        checked = prefs.gesturePinch,
                        place = RowPlace.Middle,
                        onCheckedChange = { prefs.updateGesturePinch(it) },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Schedule,
                        title = stringResource(R.string.dream),
                        subtitle = stringResource(R.string.dream_hint),
                        place = RowPlace.Middle,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_DREAM_SETTINGS)
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Accessibility,
                        title = stringResource(R.string.gestures_service),
                        subtitle = stringResource(R.string.gestures_service_hint),
                        place = RowPlace.Last,
                        onClick = {
                            runCatching {
                                context.startActivity(app.plein.data.PleinGestures.settingsIntent())
                            }
                        },
                    )
                }
            }

            if (selfUpdating) {
                item {
                    SettingsSection(stringResource(R.string.updates)) {
                        SettingsRow(
                            icon = Icons.Rounded.Download,
                            title = stringResource(R.string.check_updates),
                            subtitle = updateState.ifEmpty { "Plein $versionName" },
                            place = RowPlace.First,
                            onClick = onCheckUpdates,
                        )
                        SettingsToggleRow(
                            icon = Icons.Rounded.Schedule,
                            title = stringResource(R.string.auto_check),
                            subtitle = stringResource(R.string.auto_check_hint),
                            checked = prefs.autoUpdateCheck,
                            place = RowPlace.Last,
                            onCheckedChange = { prefs.updateAutoUpdateCheck(it) },
                        )
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.about)) {
                    SettingsRow(
                        icon = Icons.Rounded.Code,
                        title = stringResource(R.string.open_source),
                        subtitle = "Plein $versionName · GPL-3.0",
                        place = RowPlace.First,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Link,
                        title = stringResource(R.string.source_github),
                        subtitle = "github.com/THET1ME-1/Plein",
                        place = RowPlace.Last,
                        onClick = onOpenSource,
                    )
                }
            }
        }
    }

    if (pickingLanguage) {
        ChoiceSheet(
            title = stringResource(R.string.language),
            options = app.plein.data.Language.supported.map { it to app.plein.data.Language.titleOf(it) },
            selected = prefs.language,
            onPick = { code ->
                prefs.updateLanguage(code)
                app.plein.data.Language.apply(context, code)
            },
            onDismiss = { pickingLanguage = false },
        )
    }

    if (pickingWeatherApp) {
        AppPickerSheet(
            title = stringResource(R.string.weather_app),
            apps = installed,
            repository = repository,
            iconShape = prefs.iconShape,
            iconPack = prefs.iconPack,
            selected = prefs.weatherApp,
            onPick = { prefs.updateWeatherApp(it) },
            onDismiss = { pickingWeatherApp = false },
        )
    }

    if (pickingWeb) {
        ChoiceSheet(
            title = stringResource(R.string.web_provider),
            options = listOf(
                "google" to stringResource(R.string.web_google),
                "ddg" to stringResource(R.string.web_ddg),
                "bing" to stringResource(R.string.web_bing),
                "yandex" to stringResource(R.string.web_yandex),
                "startpage" to stringResource(R.string.web_startpage),
            ),
            selected = prefs.webProvider,
            onPick = { prefs.updateWebProvider(it) },
            onDismiss = { pickingWeb = false },
        )
    }

    if (choosingWallpaper) {
        ChoiceSheet(
            title = stringResource(R.string.set_wallpaper),
            options = listOf(
                "home" to stringResource(R.string.wallpaper_home),
                "lock" to stringResource(R.string.wallpaper_lock),
                "both" to stringResource(R.string.wallpaper_both),
            ),
            selected = "",
            onPick = { value ->
                onSetWallpaper(
                    when (value) {
                        "lock" -> app.plein.data.WallpaperTarget.Lock
                        "both" -> app.plein.data.WallpaperTarget.Both
                        else -> app.plein.data.WallpaperTarget.Home
                    }
                )
            },
            onDismiss = { choosingWallpaper = false },
        )
    }

    if (picking) {
        ColorPickerSheet(
            onPick = { prefs.updateSeedColor(it) },
            onDismiss = { picking = false },
        )
    }

    pickingFontFor?.let { target ->
        FontPickerSheet(
            current = if (target == "clock") prefs.clockFont else prefs.interfaceFont,
            onPick = { family ->
                if (target == "clock") prefs.updateClockFont(family) else prefs.updateInterfaceFont(family)
            },
            onDismiss = { pickingFontFor = null },
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

@Composable
private fun IconAction(
    icon: ImageVector,
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
internal fun ShapeCell(option: IconShape, selected: Boolean, onClick: () -> Unit) {
    val haptics = app.plein.ui.rememberHaptics()
    // Выбранная форма чуть крупнее и приходит пружиной: видно, что нажатие
    // дошло, даже когда две формы похожи.
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.4f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        ),
        label = "shape",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            if (!selected) haptics.toggle(true)
            onClick()
        },
    ) {
        // Фигура растёт внутри клетки с запасом: раньше она масштабировалась
        // до края ячейки, и у выбранной срезало макушку с подбородком.
        Box(
            Modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) {
        Box(
            Modifier
                .size(54.dp)
                .scale(scale)
                .clip(option.shape())
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                )
        )
        }
        Text(
            text = stringResource(option.titleRes),
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
            PlainField(
                value = value,
                onValueChange = onValueChange,
                placeholder = title,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                androidx.compose.material3.FilledTonalButton(
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


/** Список выбора листом снизу: язык, приложение, что угодно парами. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSheet(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    searchable: Boolean = false,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val shown = remember(query, options) {
        if (query.isBlank()) options
        else options.filter { it.second.contains(query, ignoreCase = true) || it.first.contains(query, ignoreCase = true) }
    }
    androidx.compose.material3.ModalBottomSheet(
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
                modifier = Modifier.padding(start = 22.dp, bottom = 10.dp),
            )
            if (searchable) {
                PlainSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            LazyColumn(Modifier.height(430.dp)) {
                items(shown.size, key = { shown[it].first + it }) { index ->
                    val (value, label) = shown[index]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(value); onDismiss() }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = if (value == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (value == selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Четыре формы для снимка: первая выбрана, как на экране настроек. */
@Composable
fun ShapesPreviewPanel() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconShape.featured.forEachIndexed { index, option ->
            ShapeCell(option = option, selected = index == 0, onClick = {})
        }
    }
}
