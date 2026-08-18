package app.plein

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import app.plein.ui.theme.entering
import app.plein.ui.theme.exiting
import app.plein.ui.theme.isDark
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Как часто перезапрашивается погода, пока лаунчер открыт. */
private const val WEATHER_REFRESH_MS = 30 * 60 * 1000L

/** Название плитки для листов выбора. */
private fun tileTitle(kind: String): Int = when (kind) {
    app.plein.ui.home.Tiles.CLOCK -> R.string.tile_clock
    app.plein.ui.home.Tiles.WEATHER -> R.string.tile_weather
    app.plein.ui.home.Tiles.BATTERY -> R.string.tile_battery
    app.plein.ui.home.Tiles.CALENDAR -> R.string.tile_calendar
    else -> R.string.tile_note
}

/** Экраны лаунчера. Один активен за раз, домашний всегда под ними. */
private enum class Screen { Home, Search, Settings, Overview }

/**
 * Предок — FragmentActivity, а не ComponentActivity: `BiometricPrompt` умеет
 * работать только с ним, а замок на скрытую папку нужен именно системный.
 */
class MainActivity : androidx.fragment.app.FragmentActivity() {


    /** Нажатие «Домой», пока лаунчер уже открыт: считаем щелчки. */
    private val homeTicks = kotlinx.coroutines.flow.MutableStateFlow(0)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(android.content.Intent.CATEGORY_HOME)) {
            homeTicks.value = homeTicks.value + 1
        }
    }

    private lateinit var repository: AppRepository
    private lateinit var prefs: Prefs
    private lateinit var folderStore: FolderStore
    private lateinit var backdropSource: BackdropSource
    private lateinit var layoutStore: app.plein.data.LayoutStore
    private lateinit var widgets: app.plein.data.Widgets
    private lateinit var hiddenApps: app.plein.data.HiddenApps
    private lateinit var backdropLibrary: app.plein.data.BackdropLibrary
    private lateinit var backdropPicker: app.plein.data.BackdropPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = AppRepository(this)
        prefs = Prefs(this)
        folderStore = FolderStore(this)
        backdropSource = BackdropSource(this)
        layoutStore = app.plein.data.LayoutStore(this)
        widgets = app.plein.data.Widgets(this)
        hiddenApps = app.plein.data.HiddenApps(this)
        backdropLibrary = app.plein.data.BackdropLibrary(this)
        backdropPicker = app.plein.data.BackdropPicker(
            context = this,
            prefs = prefs,
            library = backdropLibrary,
            source = backdropSource,
            wikimedia = app.plein.data.WikimediaSource(this),
            screenWidth = resources.displayMetrics.widthPixels,
        )
        repository.start()
        settleWidgets()

        setContent {
            val context = LocalContext.current
            val allApps by repository.apps.collectAsState()
            // Спрятанные не попадают ни в сетку, ни в поиск, ни в выбор
            // приложения погоды: список фильтруется один раз здесь.
            val apps = remember(allApps, hiddenApps.keys, hiddenApps.unlocked) {
                if (hiddenApps.unlocked) allApps else hiddenApps.visible(allApps)
            }
            val dark = prefs.themeMode.isDark(isSystemInDarkTheme())

            var seed by remember { mutableStateOf(DefaultSeed) }
            // Кадр поднимаем с диска: нажатие «Домой» и убийство процесса
            // возвращали вшитую фотографию вместо скачанной.
            var backdrop by remember {
                mutableStateOf(prefs.savedBackdrop() ?: Backdrops.firstFor(dark))
            }
            var screen by remember { mutableStateOf(Screen.Home) }
            var menuFor by remember { mutableStateOf<AppEntry?>(null) }
            var editing by remember { mutableStateOf(false) }
            var isDefault by remember { mutableStateOf(DefaultLauncher.isDefault(context)) }
            var loadingBackdrop by remember { mutableStateOf(false) }
            var backdropProgress by remember { mutableFloatStateOf(0f) }
            var backdropFailure by remember { mutableStateOf<String?>(null) }
            var tileMenu by remember { mutableStateOf<Pair<String, app.plein.data.CellItem>?>(null) }
            var editingNote by remember { mutableStateOf(false) }
            var addingTileTo by remember { mutableStateOf<String?>(null) }
            var pickingWidgetFor by remember { mutableStateOf<String?>(null) }
            var pendingWidget by remember { mutableStateOf<Triple<String, Int, Pair<Int, Int>>?>(null) }
            var weatherTemp by remember { mutableStateOf<String?>(null) }
            var weatherCode by remember { mutableIntStateOf(0) }
            var weatherTick by remember { mutableIntStateOf(0) }

            val locationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> if (granted) weatherTick++ }

            // Разрешение спрашиваем один раз за установку: отказавшийся
            // получал системный запрос при каждом входе на домашний экран,
            // пока Android не начинал глушить их сам.
            LaunchedEffect(prefs.showWeather) {
                if (prefs.showWeather && !prefs.weatherAsked &&
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    prefs.markWeatherAsked()
                    locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }

            // Погода обновляется при показе экрана, при включении и дальше
            // сама раз в полчаса: до этого температура застывала на той, что
            // приехала при входе, и висела до перезапуска лаунчера.
            LaunchedEffect(prefs.showWeather, prefs.weatherProvider, weatherTick) {
                if (!prefs.showWeather) {
                    weatherTemp = null
                    return@LaunchedEffect
                }
                while (true) {
                    val now = app.plein.data.Weather(context).current(prefs.weatherProvider)
                    weatherTemp = now?.let { "${it.celsius}°" }
                    weatherCode = now?.code ?: 0
                    kotlinx.coroutines.delay(WEATHER_REFRESH_MS)
                }
            }
            val scope = rememberCoroutineScope()

            // Один запрос за раз: пока кадр едет, кнопка и жест молчат. Иначе
            // два ответа подряд перебивали друг друга, а индикатор не гас.
            val loadBackdrop: () -> Unit = {
                if (!loadingBackdrop) {
                    scope.launch {
                        loadingBackdrop = true
                        try {
                            backdropProgress = 0f
                            // Потолок по времени: без него зависший запрос
                            // оставлял индикатор навсегда, и жест больше не
                            // запускал загрузку.
                            val fresh = kotlinx.coroutines.withTimeoutOrNull(45_000) {
                                backdropPicker.next(backdrop, dark, weatherCode) { done ->
                                    backdropProgress = done
                                }
                            }
                            if (fresh != null) {
                                backdrop = fresh
                                prefs.saveBackdrop(backdrop)
                            }
                            backdropFailure = backdropPicker.lastFailure
                                ?: if (fresh == null) "сеть не ответила за 45 секунд" else null
                            backdropFailure?.let { reason ->
                                android.widget.Toast.makeText(
                                    context, reason, android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                        } finally {
                            loadingBackdrop = false
                            backdropProgress = 0f
                        }
                    }
                }
            }

            // Роль запрашивается через результат: startActivity системный диалог
            // показывает не всегда, а вернуться надо с обновлённым состоянием.
            val roleLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { isDefault = DefaultLauncher.isDefault(context) }

            val haptics = app.plein.ui.rememberHaptics()

            // Системный запрос на привязку виджета: разрешил — ставим, отказал —
            // отпускаем выданный номер, иначе он повиснет за нами навсегда.
            val bindLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val waiting = pendingWidget ?: return@rememberLauncherForActivityResult
                pendingWidget = null
                widgets.clearPending()
                val (folderId, widgetId, size) = waiting
                if (result.resultCode == RESULT_OK) {
                    layoutStore.addWidget(folderId, widgetId, size.first, size.second, prefs.columns)
                    haptics.confirm()
                } else {
                    widgets.release(widgetId)
                    haptics.reject()
                }
            }

            // Голос слушаем сами: на прошивке без сервисов Google активности
            // распознавания нет вовсе, а служба есть — значит чужого окна не
            // будет, и рисуем своё.
            var spokenQuery by remember { mutableStateOf("") }
            var listening by remember { mutableStateOf(false) }
            val voice = remember { app.plein.data.VoiceInput(context) }
            val voiceReady = remember { app.plein.data.VoiceInput.available(context) }

            val beginListening: () -> Unit = {
                listening = true
                voice.start { spoken ->
                    spokenQuery = spoken
                    listening = false
                    screen = Screen.Search
                    haptics.confirm()
                }
            }

            val micPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> if (granted) beginListening() else haptics.reject() }

            val startVoice: (() -> Unit)? = if (voiceReady) {
                {
                    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.RECORD_AUDIO,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (granted) beginListening() else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            } else if (app.plein.data.Voice.assistantAvailable(context)) {
                { runCatching { startActivity(app.plein.data.Voice.assistantIntent()) }; Unit }
            } else {
                null
            }

            DisposableEffect(Unit) { onDispose { voice.stop() } }

            // Обновление живёт целиком в лаунчере: проверка, скачивание своего
            // куска по ABI и системный установщик. Под Obtainium и магазинами
            // раздел скрыт, там обновляет тот, кто поставил.
            val selfUpdating = remember { app.plein.data.Updates.selfUpdating(context) }
            var updateState by remember { mutableStateOf("") }
            var pendingUpdate by remember { mutableStateOf<app.plein.data.Update?>(null) }

            val runUpdate: (app.plein.data.Update) -> Unit = { update ->
                scope.launch {
                    updateState = getString(R.string.update_downloading, 0)
                    val file = app.plein.data.Updates.download(context, update) { done ->
                        updateState = getString(R.string.update_downloading, (done * 100).toInt())
                    }
                    if (file == null) {
                        updateState = getString(R.string.update_failed)
                        haptics.reject()
                        return@launch
                    }
                    if (!app.plein.data.Updates.canInstall(context)) {
                        runCatching { startActivity(app.plein.data.Updates.permissionIntent(context)) }
                        return@launch
                    }
                    val started = app.plein.data.Updates.install(context, file)
                    if (!started) {
                        updateState = getString(R.string.update_failed)
                        haptics.reject()
                    }
                }
            }

            val checkUpdates: (Boolean) -> Unit = { loud ->
                scope.launch {
                    if (loud) updateState = getString(R.string.updating)
                    val found = app.plein.data.Updates.check(context, BuildConfig.VERSION_NAME)
                    prefs.lastUpdateCheck = System.currentTimeMillis()
                    when {
                        found != null -> {
                            pendingUpdate = found
                            updateState = getString(R.string.update_available, found.version)
                            haptics.confirm()
                        }
                        loud -> updateState = getString(R.string.up_to_date)
                        else -> updateState = ""
                    }
                }
            }

            // Тихая проверка не чаще раза в сутки: лаунчер открывают десятки
            // раз на дню, и стучаться в сеть на каждом входе незачем.
            LaunchedEffect(selfUpdating, prefs.autoUpdateCheck) {
                if (!selfUpdating || !prefs.autoUpdateCheck) return@LaunchedEffect
                val day = 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - prefs.lastUpdateCheck < day) return@LaunchedEffect
                delay(4000)
                checkUpdates(false)
            }

            // Копия ходит через системный выбор файла: лаунчеру не нужен доступ
            // ко всей памяти, человек сам говорит, куда положить и что взять.
            // Фото из галереи: системный выбор не просит доступа ко всей памяти.
            val photoLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.PickMultipleVisualMedia(20)
            ) { uris ->
                if (uris.isEmpty()) return@rememberLauncherForActivityResult
                scope.launch {
                    var last: app.plein.data.Backdrop? = null
                    uris.forEach { uri -> backdropLibrary.addFromGallery(uri)?.let { last = it } }
                    prefs.updateBackdropOrigin(app.plein.data.BackdropOrigin.Gallery)
                    last?.let {
                        backdrop = it
                        prefs.saveBackdrop(it)
                    }
                    haptics.confirm()
                }
            }

            // Папка целиком: права на дерево держим постоянными, иначе после
            // перезагрузки лаунчер потеряет доступ к своим же обоям.
            val folderLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                prefs.updateBackdropFolder(uri.toString())
                prefs.updateBackdropOrigin(app.plein.data.BackdropOrigin.Folder)
                haptics.confirm()
                loadBackdrop()
            }

            // Папка под копии: права держим постоянными, иначе после
            // перезагрузки лаунчер потеряет доступ к своему же архиву.
            val backupFolderLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
                prefs.updateBackupFolder(uri.toString())
                prefs.updateAutoBackup(true)
                haptics.confirm()
            }

            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val done = app.plein.data.Backup.export(context, uri)
                if (done) haptics.confirm() else haptics.reject()
                android.widget.Toast.makeText(
                    context,
                    getString(if (done) R.string.backup_saved else R.string.backup_failed),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }

            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val done = app.plein.data.Backup.import(context, uri)
                android.widget.Toast.makeText(
                    context,
                    getString(if (done) R.string.backup_restored else R.string.backup_failed),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
                if (done) {
                    haptics.confirm()
                    // Настройки читаются один раз при создании, поэтому экран
                    // пересобираем целиком: иначе половина осталась бы старой.
                    recreate()
                } else {
                    haptics.reject()
                }
            }

            // Кнопка «Домой» из любого экрана возвращает на главный.
            val homeTick by homeTicks.collectAsState()
            LaunchedEffect(homeTick) {
                if (homeTick > 0) {
                    screen = Screen.Home
                    menuFor = null
                    editing = false
                }
            }

            // Замок защёлкивается, как только экран уходит: иначе «спрятано»
            // означало бы «спрятано до первого раза».
            val lockOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            androidx.lifecycle.compose.LifecycleEventEffect(
                androidx.lifecycle.Lifecycle.Event.ON_STOP,
                lockOwner,
            ) { hiddenApps.lock() }

            // Кадр меняется на возвращении, а не под руками: человек ушёл в
            // приложение и вернулся — дома его встречает новый вид.
            val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(lifecycle, prefs.backdropOnReturn) {
                if (!prefs.backdropOnReturn) return@LaunchedEffect
                var first = true
                lifecycle.currentStateFlow.collect { state ->
                    if (state == androidx.lifecycle.Lifecycle.State.RESUMED) {
                        if (first) first = false else loadBackdrop()
                    }
                }
            }

            LaunchedEffect(Unit) { repository.refresh() }

            // Копия делается сама, когда прошла неделя. Планировщик ради одного
            // файла в неделю — лишняя работа: лаунчер и так открывают часто.
            LaunchedEffect(prefs.autoBackup, prefs.backupFolder) {
                if (!app.plein.data.AutoBackup.due(prefs)) return@LaunchedEffect
                delay(3000)
                if (app.plein.data.AutoBackup.run(context, prefs)) {
                    android.widget.Toast.makeText(
                        context, getString(R.string.backup_auto_done),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            LaunchedEffect(apps) { folderStore.seedIfEmpty(apps) }

            val density = LocalDensity.current
            // Тему сменили — кадр меняем, только если он ей больше не годится.
            // Безусловная замена стирала скачанный кадр на первом же кадре
            // композиции, ещё до того, как человек что-то трогал.
            LaunchedEffect(dark) {
                if (!Backdrops.fits(backdrop, dark)) backdrop = Backdrops.firstFor(dark)
            }

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
                // Значки грузим все разом: ленивая подгрузка дёргала кадры на
                // скролле. Прогрев стоит внутри темы: цвета монохрома обязаны
                // совпасть с теми, что возьмёт значок, иначе кэш наполнится
                // картинками, которых никто не спросит.
                val scheme = androidx.compose.material3.MaterialTheme.colorScheme
                val monoStyle = if (prefs.monoIcons == app.plein.data.MonoMode.Off) null else {
                    app.plein.data.MonoStyle(
                        mode = prefs.monoIcons,
                        tint = scheme.onSurface.toArgb(),
                        background = scheme.surfaceContainerHighest.toArgb(),
                    )
                }
                LaunchedEffect(apps, prefs.columns, prefs.iconPack, prefs.iconShape, monoStyle) {
                    if (apps.isEmpty()) return@LaunchedEffect
                    // Ждём первый кадр: иначе прогрев спорит с отрисовкой экрана.
                    withFrameNanos { }
                    delay(600)
                    val sizePx = with(density) { iconSizeFor(prefs.columns).roundToPx() }
                    repository.preloadIcons(
                        sizePx = sizePx,
                        shapeKey = prefs.iconShape.name,
                        shapePath = prefs.iconShape.path(sizePx),
                        iconPack = prefs.iconPack,
                        mono = monoStyle,
                    )
                }

                HomeScreen(
                    folders = folderStore.folders,
                    apps = apps,
                    repository = repository,
                    prefs = prefs,
                    backdrop = backdrop,
                    weatherTemp = weatherTemp,
                    weatherCode = weatherCode,
                    onPullRefresh = loadBackdrop,
                    onWeatherClick = {
                        val pkg = prefs.weatherApp
                        if (pkg.isNotEmpty()) {
                            val intent = packageManager.getLaunchIntentForPackage(pkg)
                            if (intent != null) runCatching { startActivity(intent) }
                        }
                    },
                    editing = editing,
                    // Нажатие идёт в фотобанк за новым кадром; без сети остаются вшитые.
                    onShuffleBackdrop = loadBackdrop,
                    loadingBackdrop = loadingBackdrop,
                    loadingProgress = backdropProgress,
                    onSeedExtracted = {
                        seed = it
                        // Заставка и всё, что живёт вне активности, читает
                        // цвет кадра отсюда: в памяти он им не виден.
                        prefs.updatePhotoSeed(it.toArgb())
                    },
                    onOpenSearch = { screen = Screen.Search },
                    hiddenCount = hiddenApps.keys.size,
                    hiddenUnlocked = hiddenApps.unlocked,
                    hiddenApps = remember(allApps, hiddenApps.keys) { hiddenApps.hidden(allApps) },
                    onUnlockHidden = {
                        app.plein.data.Lock.ask(
                            activity = this@MainActivity,
                            title = getString(R.string.hidden_prompt),
                            subtitle = getString(R.string.hidden_prompt_hint),
                            onSuccess = {
                                hiddenApps.unlocked = true
                                haptics.confirm()
                            },
                            onFail = { haptics.reject() },
                        )
                    },
                    onVoice = startVoice,
                    tilesOf = { folderId -> layoutStore.tiles(folderId) },
                    onTileMove = { folderId, item, cell ->
                        layoutStore.move(folderId, item, cell, prefs.columns)
                    },
                    onTileMenu = { folderId, item -> tileMenu = folderId to item },
                    onTileRemove = { folderId, item ->
                        // Номер виджета возвращаем системе: иначе он числится
                        // за нами и после удаления.
                        (item as? app.plein.data.CellItem.Widget)?.let { widgets.release(it.widgetId) }
                        layoutStore.remove(folderId, item)
                    },
                    onAddTile = { folderId -> addingTileTo = folderId },
                    widgetContent = { widgetId, width, height ->
                        app.plein.ui.home.WidgetView(
                            widgets = widgets,
                            widgetId = widgetId,
                            widthCells = width,
                            heightCells = height,
                            columns = prefs.columns,
                            rowHeightDp = app.plein.ui.home.CellMetrics.resolve(
                                custom = prefs.rowHeight,
                                columns = prefs.columns,
                                showLabels = prefs.showLabels,
                            ).value.toInt(),
                        )
                    },
                    onTileAction = { kind ->
                        // Короткое касание плитки ведёт в её приложение;
                        // заметка правится на месте.
                        when (kind) {
                            app.plein.ui.home.Tiles.CLOCK -> runCatching {
                                startActivity(android.content.Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS))
                            }

                            app.plein.ui.home.Tiles.WEATHER -> {
                                val pkg = prefs.weatherApp
                                if (pkg.isNotEmpty()) {
                                    packageManager.getLaunchIntentForPackage(pkg)?.let { intent ->
                                        runCatching { startActivity(intent) }
                                    }
                                }
                            }

                            app.plein.ui.home.Tiles.NOTE -> editingNote = true

                            app.plein.ui.home.Tiles.CALENDAR -> runCatching {
                                startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW).setData(
                                        android.provider.CalendarContract.CONTENT_URI.buildUpon()
                                            .appendPath("time").build()
                                    )
                                )
                            }

                            else -> Unit
                        }
                        Unit
                    },
                    tileContent = { kind ->
                        // Плитки живут на настоящих данных: время идёт, заряд
                        // меняется, погода та же, что на кадре.
                        when (kind) {
                            app.plein.ui.home.Tiles.CLOCK -> {
                                val now = app.plein.ui.home.rememberNow()
                                val locale = app.plein.ui.rememberLocale()
                                app.plein.ui.home.ClockTile(
                                    time = java.text.SimpleDateFormat(
                                        if (prefs.clockTwentyFour) "HH:mm" else "h:mm a", locale,
                                    ).format(now),
                                    date = java.text.SimpleDateFormat("EEE, d MMM", locale)
                                        .format(now).uppercase(locale),
                                )
                            }

                            app.plein.ui.home.Tiles.WEATHER -> app.plein.ui.home.WeatherTile(
                                temperature = weatherTemp,
                                code = weatherCode,
                                place = prefs.weatherProvider,
                            )

                            app.plein.ui.home.Tiles.BATTERY -> {
                                val battery = app.plein.data.rememberBattery()
                                app.plein.ui.home.BatteryTile(
                                    percent = battery.percent,
                                    charging = battery.charging,
                                )
                            }

                            app.plein.ui.home.Tiles.NOTE -> app.plein.ui.home.NoteTile(
                                text = prefs.noteText.ifEmpty { getString(R.string.note_hint) },
                            )

                            else -> app.plein.ui.home.CalendarTile(
                                title = getString(R.string.no_events),
                                time = "",
                            )
                        }
                    },
                    onDoubleTap = {
                        if (!app.plein.data.PleinGestures.lockScreen()) {
                            runCatching { startActivity(app.plein.data.PleinGestures.settingsIntent()) }
                        } else {
                            haptics.confirm()
                        }
                    },
                    onShade = {
                        if (prefs.gestureShade && !app.plein.data.PleinGestures.notifications()) {
                            runCatching { startActivity(app.plein.data.PleinGestures.settingsIntent()) }
                        }
                    },
                    onOverview = {
                        haptics.longPress()
                        screen = Screen.Overview
                    },
                    onOpenSettings = {
                        isDefault = DefaultLauncher.isDefault(context)
                        screen = Screen.Settings
                    },
                    onAppMenu = { menuFor = it },
                    onReorder = { folder, ordered ->
                        folderStore.setOrder(folder.id, ordered.map { it.key })
                    },
                    onReorderKeys = { folder, keys -> folderStore.setOrder(folder.id, keys) },
                    onFinishEditing = { editing = false },
                    onStartEditing = { editing = true },
                )

                // Экраны приезжают снизу и уходят обратно: без перехода
                // поиск возникал поверх дома щелчком, будто подменили картинку.
                AnimatedVisibility(
                    visible = screen == Screen.Search,
                    enter = slideInVertically(entering()) { it / 6 } + fadeIn(entering()),
                    exit = slideOutVertically(exiting()) { it / 8 } + fadeOut(exiting()),
                ) {
                    SearchScreen(
                        apps = apps,
                        repository = repository,
                        iconShape = prefs.iconShape,
                        webProvider = prefs.webProvider,
                        initialQuery = spokenQuery,
                        searchContacts = prefs.searchContacts,
                        contactsAsked = prefs.contactsAsked,
                        onContactsAsked = { prefs.markContactsAsked() },
                        onVoice = startVoice,
                        onAppMenu = { menuFor = it },
                        onClose = {
                            screen = Screen.Home
                            spokenQuery = ""
                        },
                    )
                }

                AnimatedVisibility(
                    visible = screen == Screen.Overview,
                    enter = fadeIn(entering()),
                    exit = fadeOut(exiting()),
                ) {
                    app.plein.ui.home.FoldersOverview(
                        folders = folderStore.folders,
                        apps = apps,
                        repository = repository,
                        iconShape = prefs.iconShape,
                        iconPack = prefs.iconPack,
                        monoMode = prefs.monoIcons,
                        current = 0,
                        onPick = { screen = Screen.Home },
                        onClose = { screen = Screen.Home },
                    )
                }

                AnimatedVisibility(
                    visible = screen == Screen.Settings,
                    enter = slideInVertically(entering()) { it / 6 } + fadeIn(entering()),
                    exit = slideOutVertically(exiting()) { it / 8 } + fadeOut(exiting()),
                ) {
                    SettingsScreen(
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
                        onExportBackup = { exportLauncher.launch(app.plein.data.Backup.fileName()) },
                        onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        backdropFailure = backdropFailure,
                        onPickPhotos = {
                            photoLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onPickFolder = { folderLauncher.launch(null) },
                        onPickBackupFolder = { backupFolderLauncher.launch(null) },
                        selfUpdating = selfUpdating,
                        updateState = updateState,
                        onCheckUpdates = {
                            val ready = pendingUpdate
                            if (ready != null) runUpdate(ready) else checkUpdates(true)
                        },
                        onSetWallpaper = { target ->
                            scope.launch {
                                val done = app.plein.data.Wallpapers.set(context, backdrop, target)
                                if (done) haptics.confirm() else haptics.reject()
                                android.widget.Toast.makeText(
                                    context,
                                    getString(if (done) R.string.wallpaper_done else R.string.wallpaper_failed),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }

                // Плитку добавляют из режима правки, убирают и меняют размер
                // из её же меню — отдельного экрана для этого не нужно.
                pickingWidgetFor?.let { folderId ->
                    app.plein.ui.settings.WidgetPickerSheet(
                        widgets = widgets,
                        columns = prefs.columns,
                        rowHeightDp = app.plein.ui.home.CellMetrics.resolve(
                            custom = prefs.rowHeight,
                            columns = prefs.columns,
                            showLabels = prefs.showLabels,
                        ).value.toInt(),
                        onPick = { provider, width, height ->
                            // Привязка в два шага: сперва номер, потом право его
                            // использовать. Молча можно не везде, и тогда
                            // система показывает свой запрос.
                            val widgetId = widgets.allocateId()
                            if (widgets.bind(widgetId, provider.info)) {
                                layoutStore.addWidget(folderId, widgetId, width, height, prefs.columns)
                                haptics.confirm()
                            } else {
                                pendingWidget = Triple(folderId, widgetId, width to height)
                                // Ожидание дублируется на диск: пока открыт
                                // системный диалог, лаунчер в фоне и его могут
                                // убить вместе со всем состоянием экрана.
                                widgets.rememberPending(folderId, widgetId, width, height)
                                bindLauncher.launch(widgets.bindIntent(widgetId, provider.info))
                            }
                            pickingWidgetFor = null
                        },
                        onDismiss = { pickingWidgetFor = null },
                    )
                }

                addingTileTo?.let { folderId ->
                    app.plein.ui.settings.ChoiceSheetPublic(
                        title = getString(R.string.add_tile),
                        options = app.plein.ui.home.Tiles.all.map { kind ->
                            kind to getString(tileTitle(kind))
                        } + listOf("widget" to getString(R.string.add_widget)),
                        selected = "",
                        onPick = { kind ->
                            if (kind == "widget") {
                                pickingWidgetFor = folderId
                            } else {
                                layoutStore.add(folderId, kind, prefs.columns)
                                haptics.confirm()
                            }
                        },
                        onDismiss = { addingTileTo = null },
                    )
                }

                tileMenu?.let { (folderId, item) ->
                    val kind = (item as? app.plein.data.CellItem.Tile)?.kind.orEmpty()
                    val sizes = if (item is app.plein.data.CellItem.Widget) {
                        // Виджету размеры перебираем руками: приложение назвало
                        // минимум, а больше человек решает сам.
                        listOf(2 to 2, 4 to 2, 4 to 1, 2 to 1, 4 to 4)
                    } else {
                        app.plein.data.TileSizes.variants(kind)
                    }
                    app.plein.ui.settings.ChoiceSheetPublic(
                        title = if (item is app.plein.data.CellItem.Widget) getString(R.string.widgets)
                        else getString(tileTitle(kind)),
                        options = sizes.map { (w, h) -> "$w:$h" to "$w × $h" } +
                            listOf("remove" to getString(R.string.tile_remove)),
                        selected = "",
                        onPick = { choice ->
                            if (choice == "remove") {
                                (item as? app.plein.data.CellItem.Widget)?.let { widgets.release(it.widgetId) }
                                layoutStore.remove(folderId, item)
                            } else {
                                val (w, h) = choice.split(':').map { it.toInt() }
                                layoutStore.resize(folderId, item, w, h, prefs.columns)
                            }
                            haptics.confirm()
                        },
                        onDismiss = { tileMenu = null },
                    )
                }

                if (listening) {
                    app.plein.ui.search.VoiceSheet(
                        voice = voice,
                        onDismiss = {
                            voice.stop()
                            listening = false
                        },
                    )
                }

                if (editingNote) {
                    app.plein.ui.settings.NameSheetPublic(
                        title = getString(R.string.tile_note),
                        value = prefs.noteText,
                        onValueChange = { prefs.updateNoteText(it) },
                        onConfirm = { editingNote = false },
                        onDismiss = { editingNote = false },
                    )
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
                        hidden = hiddenApps.contains(entry.key),
                        onToggleHidden = {
                            if (hiddenApps.contains(entry.key)) {
                                hiddenApps.toggle(entry.key)
                            } else {
                                hiddenApps.toggle(entry.key)
                                haptics.confirm()
                            }
                        },
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

    /**
     * Разбор после убийства процесса: виджет, который успели разрешить, но не
     * успели поставить, и номера, за которыми ничего не стоит.
     *
     * Без этого разрешённый виджет пропадал молча, а его номер оставался за
     * лаунчером навсегда — так их и копится десяток за пару месяцев.
     */
    private fun settleWidgets() {
        widgets.pending()?.let { waiting ->
            widgets.clearPending()
            if (widgets.isBound(waiting.widgetId)) {
                layoutStore.addWidget(
                    waiting.folderId, waiting.widgetId,
                    waiting.width, waiting.height, prefs.columns,
                )
            } else {
                widgets.release(waiting.widgetId)
            }
        }
        widgets.releaseOrphans(layoutStore.widgetIds())
    }

    override fun onStart() {
        super.onStart()
        // Хост слушает обновления, только пока лаунчер на виду: иначе
        // приложения рисуют в пустоту и тратят батарею.
        widgets.startListening()
    }

    override fun onStop() {
        widgets.stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        repository.stop()
        super.onDestroy()
    }
}
