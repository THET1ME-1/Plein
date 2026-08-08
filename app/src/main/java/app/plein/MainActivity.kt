package app.plein

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.Backdrops
import app.plein.data.DefaultLauncher
import app.plein.data.FolderStore
import app.plein.data.Prefs
import app.plein.ui.home.HomeScreen
import app.plein.ui.menu.AppMenuSheet
import app.plein.ui.search.SearchScreen
import app.plein.ui.settings.SettingsScreen
import app.plein.ui.theme.DefaultSeed
import app.plein.ui.theme.PleinTheme
import app.plein.ui.theme.isDark
import androidx.compose.ui.graphics.Color

/** Экраны лаунчера. Один активен за раз, домашний всегда под ними. */
private enum class Screen { Home, Search, Settings }

class MainActivity : ComponentActivity() {

    private lateinit var repository: AppRepository
    private lateinit var prefs: Prefs
    private lateinit var folderStore: FolderStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = AppRepository(this)
        prefs = Prefs(this)
        folderStore = FolderStore(this)
        repository.start()

        setContent {
            val context = LocalContext.current
            val apps by repository.apps.collectAsState()
            val dark = prefs.themeMode.isDark(isSystemInDarkTheme())

            var seed by remember { mutableStateOf(DefaultSeed) }
            var backdrop by remember { mutableStateOf(Backdrops.firstFor(dark)) }
            var screen by remember { mutableStateOf(Screen.Home) }
            var menuFor by remember { mutableStateOf<AppEntry?>(null) }
            var editing by remember { mutableStateOf(false) }
            var isDefault by remember { mutableStateOf(DefaultLauncher.isDefault(context)) }

            // Роль запрашивается через результат: startActivity системный диалог
            // показывает не всегда, а вернуться надо с обновлённым состоянием.
            val roleLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { isDefault = DefaultLauncher.isDefault(context) }

            LaunchedEffect(Unit) { repository.refresh() }
            LaunchedEffect(apps) { folderStore.seedIfEmpty(apps) }
            LaunchedEffect(dark) { backdrop = Backdrops.firstFor(dark) }

            // Цвет из кадра перебивает свой seed, пока это не выключено в настройках.
            val activeSeed = if (prefs.seedFromPhoto) seed else Color(prefs.seedColor)

            PleinTheme(
                dark = dark,
                seed = activeSeed,
                dynamicColor = prefs.dynamicColor,
                amoled = prefs.amoled,
                vibrancy = prefs.vibrancy,
            ) {
                HomeScreen(
                    folders = folderStore.folders,
                    apps = apps,
                    repository = repository,
                    prefs = prefs,
                    backdrop = backdrop,
                    editing = editing,
                    onShuffleBackdrop = { backdrop = Backdrops.next(backdrop, dark) },
                    onSeedExtracted = { seed = it },
                    onOpenSearch = { screen = Screen.Search },
                    onOpenSettings = {
                        isDefault = DefaultLauncher.isDefault(context)
                        screen = Screen.Settings
                    },
                    onAppMenu = { menuFor = it },
                    onReorder = { folder, ordered ->
                        folderStore.setOrder(folder.id, ordered.map { it.key })
                    },
                    onFinishEditing = { editing = false },
                )

                when (screen) {
                    Screen.Search -> SearchScreen(
                        apps = apps,
                        repository = repository,
                        iconShape = prefs.iconShape.shape(),
                        onAppMenu = { menuFor = it },
                        onClose = { screen = Screen.Home },
                    )

                    Screen.Settings -> SettingsScreen(
                        prefs = prefs,
                        folders = folderStore.folders,
                        isDefaultLauncher = isDefault,
                        dark = dark,
                        onClose = { screen = Screen.Home },
                        onMakeDefault = { roleLauncher.launch(DefaultLauncher.requestIntent(context)) },
                        onCreateFolder = { folderStore.create(it) },
                        onRenameFolder = { id, title -> folderStore.rename(id, title) },
                        onDeleteFolder = { folderStore.delete(it) },
                        onMoveFolder = { from, to -> folderStore.moveFolder(from, to) },
                    )

                    Screen.Home -> Unit
                }

                menuFor?.let { entry ->
                    AppMenuSheet(
                        entry = entry,
                        repository = repository,
                        folders = folderStore.folders,
                        memberOf = folderStore.foldersWith(entry.key),
                        iconShape = prefs.iconShape.shape(),
                        onDismiss = { menuFor = null },
                        onRename = { name ->
                            repository.setCustomLabel(entry.key, name)
                            menuFor = null
                        },
                        onToggleFolder = { folderId -> folderStore.toggleMembership(folderId, entry.key) },
                        onStartReorder = {
                            editing = true
                            menuFor = null
                        },
                    )
                }

                BackHandler(enabled = screen != Screen.Home || editing) {
                    when {
                        editing -> editing = false
                        else -> screen = Screen.Home
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        repository.stop()
        super.onDestroy()
    }
}
