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
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import app.plein.ui.home.iconSizeFor
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.BackdropSource
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
    private lateinit var backdropSource: BackdropSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = AppRepository(this)
        prefs = Prefs(this)
        folderStore = FolderStore(this)
        backdropSource = BackdropSource(this)
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
            var loadingBackdrop by remember { mutableStateOf(false) }
            var weatherTemp by remember { mutableStateOf<String?>(null) }
            var weatherCode by remember { mutableIntStateOf(0) }
            var weatherTick by remember { mutableIntStateOf(0) }

            val locationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> if (granted) weatherTick++ }

            // Разрешение спрашиваем ровно тогда, когда погоду включили.
            LaunchedEffect(prefs.showWeather) {
                if (prefs.showWeather &&
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }

            // Погода обновляется при показе экрана и когда её включили.
            LaunchedEffect(prefs.showWeather, prefs.weatherProvider, weatherTick) {
                if (!prefs.showWeather) {
                    weatherTemp = null
                } else {
                    val now = app.plein.data.Weather(context).current(prefs.weatherProvider)
                    weatherTemp = now?.let { "${it.celsius}°" }
                    weatherCode = now?.code ?: 0
                }
            }
            val scope = rememberCoroutineScope()

            // Роль запрашивается через результат: startActivity системный диалог
            // показывает не всегда, а вернуться надо с обновлённым состоянием.
            val roleLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { isDefault = DefaultLauncher.isDefault(context) }

            LaunchedEffect(Unit) { repository.refresh() }
            LaunchedEffect(apps) { folderStore.seedIfEmpty(apps) }

            // Значки грузим все разом: ленивая подгрузка дёргала кадры на скролле.
            val density = LocalDensity.current
            LaunchedEffect(apps, prefs.columns, prefs.iconPack, prefs.iconShape) {
                if (apps.isEmpty()) return@LaunchedEffect
                // Ждём первый кадр: иначе прогрев конкурирует с отрисовкой экрана.
                withFrameNanos { }
                delay(250)
                val sizePx = with(density) { iconSizeFor(prefs.columns).roundToPx() }
                repository.preloadIcons(sizePx, prefs.iconShape.name, prefs.iconShape.path(sizePx), prefs.iconPack)
            }
            LaunchedEffect(dark) { backdrop = Backdrops.firstFor(dark) }

            // Цвет из кадра перебивает свой seed, пока это не выключено в настройках.
            val activeSeed = if (prefs.seedFromPhoto) seed else Color(prefs.seedColor)

            PleinTheme(
                dark = dark,
                seed = activeSeed,
                dynamicColor = prefs.dynamicColor,
                amoled = prefs.amoled,
                vibrancy = prefs.vibrancy,
                interfaceFont = prefs.interfaceFont,
            ) {
                HomeScreen(
                    folders = folderStore.folders,
                    apps = apps,
                    repository = repository,
                    prefs = prefs,
                    backdrop = backdrop,
                    weatherTemp = weatherTemp,
                    weatherCode = weatherCode,
                    onWeatherClick = {
                        val pkg = prefs.weatherApp
                        if (pkg.isNotEmpty()) {
                            val intent = packageManager.getLaunchIntentForPackage(pkg)
                            if (intent != null) runCatching { startActivity(intent) }
                        }
                    },
                    editing = editing,
                    onShuffleBackdrop = {
                        // Каждое нажатие идёт в фотобанк за новым кадром;
                        // без сети остаются вшитые.
                        scope.launch {
                            loadingBackdrop = true
                            val fresh = backdropSource.next(dark)
                            backdrop = fresh ?: Backdrops.next(backdrop, dark)
                            loadingBackdrop = false
                        }
                    },
                    loadingBackdrop = loadingBackdrop,
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
                        iconShape = prefs.iconShape,
                        onAppMenu = { menuFor = it },
                        onClose = { screen = Screen.Home },
                    )

                    Screen.Settings -> SettingsScreen(
                        prefs = prefs,
                        repository = repository,
                        folders = folderStore.folders,
                        isDefaultLauncher = isDefault,
                        dark = dark,
                        versionName = BuildConfig.VERSION_NAME,
                        onOpenSource = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/THET1ME-1/Plein"),
                            )
                            runCatching { startActivity(intent) }
                        },
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
                        iconShape = prefs.iconShape,
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
