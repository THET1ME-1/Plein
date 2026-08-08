package app.plein.ui.home

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.Backdrop
import app.plein.data.FolderConfig
import app.plein.data.displayTitle
import app.plein.data.Prefs
import app.plein.ui.rememberHaptics
import app.plein.ui.theme.Emphasized
import app.plein.ui.theme.MonoFont
import app.plein.ui.theme.SheetCorner

private val BackdropHeight = 300.dp
private val BackdropCollapsed = 92.dp
private val SheetOverlap = 30.dp

/**
 * Полный ход оттягивания.
 *
 * Числа подобраны так, чтобы палец проходил около 260 dp — треть экрана.
 * Это уверенное протягивание, случайным движением по списку не набрать, но и
 * рука не устаёт. Прошлая резина требовала 700 dp, то есть не срабатывала.
 */
private val PullTravel = 130.dp

/** Мёртвая зона в начале хода: случайный свайп ничего не показывает. */
private const val PullDeadZone = 0.28f

/**
 * Резина оттягивания.
 *
 * Отдельно от экрана, чтобы считалась в тесте: с квадратом прирост у края
 * стремился к нулю, полный ход был недостижим, и кадр не приходил никогда.
 */
object PullPhysics {

    /** Срабатывает почти на полном ходу: последние проценты съедает резина. */
    const val TRIGGER = 0.95f

    fun resistance(reach: Float): Float = 0.8f * (1f - reach).coerceAtLeast(0.5f)

    fun accumulate(current: Float, delta: Float, limit: Float): Float {
        val reach = (current / limit).coerceIn(0f, 1f)
        return (current + delta * resistance(reach)).coerceAtMost(limit)
    }

    /** Сколько пикселей пальцем нужно, чтобы дойти до срабатывания. */
    fun travelToTrigger(limit: Float, step: Float = 12f): Float {
        var value = 0f
        var travelled = 0f
        while (value < limit * TRIGGER && travelled < 10_000f) {
            value = accumulate(value, step, limit)
            travelled += step
        }
        return travelled
    }
}

/**
 * Главный экран.
 *
 * Страницы листаются по кругу: с последней папки свайп уводит на первую.
 * Поиск и точки закреплены внизу, они живут вне листалки.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    folders: List<FolderConfig>,
    apps: List<AppEntry>,
    repository: AppRepository,
    prefs: Prefs,
    backdrop: Backdrop,
    weatherTemp: String?,
    weatherCode: Int,
    onWeatherClick: () -> Unit,
    onPullRefresh: () -> Unit,
    editing: Boolean,
    onShuffleBackdrop: () -> Unit,
    loadingBackdrop: Boolean,
    onSeedExtracted: (androidx.compose.ui.graphics.Color) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onAppMenu: (AppEntry) -> Unit,
    onReorder: (FolderConfig, List<AppEntry>) -> Unit,
    onFinishEditing: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onShade: () -> Unit = {},
    onOverview: () -> Unit = {},
    hiddenCount: Int = 0,
    hiddenUnlocked: Boolean = false,
    hiddenApps: List<AppEntry> = emptyList(),
    onUnlockHidden: () -> Unit = {},
) {
    // Страница со скрытым встаёт последней и только когда там что-то есть.
    val hasHidden = hiddenCount > 0
    val pages = (folders.size + if (hasHidden) 1 else 0).coerceAtLeast(1)
    val cyclic = pages > 1

    // Бесконечная лента: стартуем из середины, чтобы круг работал в обе стороны.
    val startPage = remember(pages) { if (cyclic) (Int.MAX_VALUE / 2) - (Int.MAX_VALUE / 2) % pages else 0 }
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { if (cyclic) Int.MAX_VALUE else 1 },
    )
    val currentPage = (pagerState.currentPage - startPage).mod(pages)

    // Сворачивающаяся шапка: кадр уезжает вдвое медленнее листа и гаснет,
    // часы уменьшаются и уходят наверх. Механика снята с Overmorrow.
    val density = LocalDensity.current
    val maxShift = with(density) { (BackdropHeight - BackdropCollapsed).toPx() }
    var shift by remember { mutableFloatStateOf(0f) }
    val progress = if (maxShift == 0f) 0f else (shift / maxShift).coerceIn(0f, 1f)

    // Ход оттягивания.
    //
    // Значение живёт обычным состоянием и меняется прямо в обработчике, а
    // возврат анимируется внутри onPreFling — она и так suspend. Раньше и
    // движение, и возврат шли через launch: отложенный snapTo прилетал после
    // animateTo, отменял его, и лист застывал оттянутым с мёртвым кругом.
    val pullLimit = with(density) { PullTravel.toPx() }
    var pull by remember { mutableFloatStateOf(0f) }

    val nested = remember(maxShift, pullLimit) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Оттянутый лист сначала встаёт на место и только потом отдаёт
                // движение списку: иначе он дёргался на полпути.
                if (available.y < 0f && pull > 0f) {
                    val next = (pull + available.y).coerceAtLeast(0f)
                    val used = next - pull
                    pull = next
                    return Offset(0f, used)
                }
                val delta = -available.y
                val next = (shift + delta).coerceIn(0f, maxShift)
                val consumed = next - shift
                shift = next
                return if (consumed != 0f) Offset(0f, -consumed) else Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Копим ход только под пальцем: на инерции список долетает до
                // верха сам, и остаток набирал полный круг без всякого жеста.
                if (source != NestedScrollSource.UserInput || shift > 0f || available.y <= 0f) {
                    return Offset.Zero
                }
                pull = PullPhysics.accumulate(pull, available.y, pullLimit)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull <= 0f) return Velocity.Zero
                // Хватает почти полного круга: добрать последние проценты
                // мешает та же резина.
                if (pull >= pullLimit * PullPhysics.TRIGGER) onPullRefresh()
                animate(
                    initialValue = pull,
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessLow),
                ) { value, _ -> pull = value }
                return Velocity.Zero
            }
        }
    }

    val haptics = rememberHaptics()

    // Щелчок на смене папки и на дотянутом жесте: рука понимает, что
    // произошло, не разглядывая экран.
    LaunchedEffect(currentPage) { haptics.tick() }
    val reached = pull >= pullLimit * PullPhysics.TRIGGER
    LaunchedEffect(reached) {
        if (reached) haptics.threshold()
    }

    val columns = prefs.columns
    val iconSize = iconSizeFor(columns)
    val iconShape = prefs.iconShape
    val iconPack = prefs.iconPack
    val monoMode = prefs.monoIcons

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(nested)
    ) {
        Backdrop(
            onSwipeDown = onShade,
            backdrop = backdrop,
            clockSize = prefs.clockSize,
            twentyFour = prefs.clockTwentyFour,
            showDate = prefs.showDate,
            weatherTemp = weatherTemp,
            weatherCode = weatherCode,
            onWeatherClick = onWeatherClick,
            clockFont = prefs.clockFont,
            onShuffle = onShuffleBackdrop,
            loading = loadingBackdrop,
            onOpenSettings = onOpenSettings,
            onSeedExtracted = onSeedExtracted,
            collapse = progress,
            pull = ((pull / pullLimit - PullDeadZone) / (PullPhysics.TRIGGER - PullDeadZone))
                .coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(BackdropHeight)
                .graphicsLayer {
                    // Фон отстаёт сильно и слегка наезжает: так глубина видна,
                    // а не читается как простое затемнение.
                    translationY = -shift * 0.28f + pull * 0.4f
                    val zoom = 1f + progress * 0.12f
                    scaleX = zoom
                    scaleY = zoom
                    alpha = 1f - progress * 0.35f
                },
        )

        // Лист идёт за пальцем и возвращается пружиной: жест ощущается
        // движением экрана, а не одним кружком в углу.
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = pull }
        ) {

            Spacer(
                Modifier.height(
                    with(density) { (BackdropHeight.toPx() - SheetOverlap.toPx() - shift).toDp() }
                )
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // Жесты слушаем на самом листе и до сетки: значки съедают
                    // касание, а лист остаётся свободным по краям и между рядов.
                    .pointerInput(prefs.gestureDoubleTapLock) {
                        if (!prefs.gestureDoubleTapLock) return@pointerInput
                        detectTapGestures(onDoubleTap = { onDoubleTap() })
                    }
                    .pointerInput(prefs.gesturePinch) {
                        if (!prefs.gesturePinch) return@pointerInput
                        var scale = 1f
                        detectTransformGestures { _, _, zoom, _ ->
                            scale *= zoom
                            // Сводим пальцы заметно, а не чуть-чуть: случайное
                            // движение по списку не должно открывать обзор.
                            if (scale < 0.72f) {
                                scale = 1f
                                onOverview()
                            }
                        }
                    },
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !editing,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val index = (page - startPage).mod(pages)
                    if (hasHidden && index == pages - 1) {
                        HiddenPage(
                            apps = hiddenApps,
                            unlocked = hiddenUnlocked,
                            count = hiddenCount,
                            repository = repository,
                            columns = columns,
                            iconSize = iconSize,
                            iconShape = iconShape,
                            iconPack = iconPack,
                            monoMode = monoMode,
                            showLabels = prefs.showLabels,
                            onUnlock = onUnlockHidden,
                            onClick = { repository.launch(it) },
                            onLongClick = onAppMenu,
                        )
                        return@HorizontalPager
                    }
                    val config = folders.getOrNull(index) ?: return@HorizontalPager
                    val folder = remember(config, apps) { config.resolve(apps) }

                    Column(Modifier.fillMaxSize()) {
                        FolderHeader(
                            title = config.displayTitle(),
                            page = index + 1,
                            pages = pages,
                            count = folder.apps.size,
                            editing = editing,
                            onFinishEditing = onFinishEditing,
                        )
                        AppsGrid(
                            apps = folder.apps,
                            repository = repository,
                            columns = columns,
                            iconSize = iconSize,
                            iconShape = iconShape,
                            iconPack = iconPack,
                            monoMode = monoMode,
                            showLabels = prefs.showLabels,
                            editing = editing,
                            onReorder = { onReorder(config, it) },
                            onClick = { repository.launch(it) },
                            onLongClick = onAppMenu,
                        )
                    }
                }
            }

            PageIndicator(style = prefs.pageIndicator, pages = pages, current = currentPage)

            SearchPill(onClick = onOpenSearch)
        }
    }
}

fun iconSizeFor(columns: Int): Dp = when (columns) {
    3 -> 82.dp
    4 -> 61.dp
    5 -> 48.dp
    else -> 40.dp
}

@Composable
private fun FolderHeader(
    title: String,
    page: Int,
    pages: Int,
    count: Int,
    editing: Boolean,
    onFinishEditing: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (editing) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onFinishEditing),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.done),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        } else {
            Text(
                text = "$page / $pages · $count",
                fontFamily = MonoFont,
                fontSize = 11.sp,
                letterSpacing = 0.9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun PageIndicator(style: String, pages: Int, current: Int) {
    // Одна страница — показывать нечего ни точками, ни полосой, ни «1 / 1».
    if (style == "none" || pages < 2) {
        Spacer(Modifier.height(12.dp))
        return
    }
    if (style == "numbers") {
        Text(
            text = "${current + 1} / $pages",
            fontFamily = MonoFont,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        return
    }
    if (style == "bar") {
        // Ход считаем анимацией, а не прыжком по страницам: полоса едет вместе
        // со свайпом. Сдвиг стоит в цепочке до заливки — иначе слой двигает
        // пустоту, а закрашенный прямоугольник остаётся на месте.
        val travel by animateFloatAsState(
            targetValue = current.toFloat(),
            animationSpec = tween(durationMillis = 280, easing = Emphasized),
            label = "indicator",
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 60.dp, vertical = 14.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(1f / pages)
                    .fillMaxHeight()
                    .graphicsLayer { translationX = size.width * travel }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        return
    }
    PageDots(pages = pages, current = current)
}

@Composable
private fun PageDots(pages: Int, current: Int) {
    if (pages < 2) {
        Spacer(Modifier.height(18.dp))
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 10.dp),
    ) {
        Spacer(Modifier.weight(1f))
        repeat(pages) { index ->
            val active = index == current
            val scale by animateFloatAsState(
                targetValue = if (active) 1f else 0.3f,
                animationSpec = tween(400, easing = Emphasized),
                label = "dot",
            )
            Box(
                Modifier
                    .width(20.dp)
                    .height(6.dp)
                    .scale(scaleX = scale, scaleY = 1f)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchPill(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
            .height(58.dp)
            .clip(CircleShape)
            .combinedClickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = stringResource(R.string.search_hint),
                fontSize = 15.sp,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            Icon(
                Icons.Rounded.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.width(14.dp))
            Icon(
                Icons.Rounded.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

/**
 * Страница со спрятанным.
 *
 * Пока замок закрыт, здесь только он и число: список не показываем даже
 * серым, иначе прятать нечего.
 */
@Composable
private fun HiddenPage(
    apps: List<AppEntry>,
    unlocked: Boolean,
    count: Int,
    repository: AppRepository,
    columns: Int,
    iconSize: Dp,
    iconShape: app.plein.ui.icons.IconShape,
    iconPack: String,
    monoMode: app.plein.data.MonoMode,
    showLabels: Boolean,
    onUnlock: () -> Unit,
    onClick: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        FolderHeader(
            title = stringResource(R.string.hidden_apps),
            page = 0,
            pages = 0,
            count = count,
            editing = false,
            onFinishEditing = {},
        )
        if (unlocked) {
            AppsGrid(
                apps = apps,
                repository = repository,
                columns = columns,
                iconSize = iconSize,
                iconShape = iconShape,
                iconPack = iconPack,
                monoMode = monoMode,
                showLabels = showLabels,
                editing = false,
                onReorder = {},
                onClick = onClick,
                onLongClick = onLongClick,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .clickable(onClick = onUnlock),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp),
                        )
                        Text(
                            text = stringResource(R.string.hidden_unlock),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
