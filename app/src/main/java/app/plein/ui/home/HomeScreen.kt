package app.plein.ui.home

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.plein.data.Prefs
import app.plein.ui.theme.Emphasized
import app.plein.ui.theme.MonoFont
import app.plein.ui.theme.SheetCorner

private val BackdropHeight = 244.dp
private val SheetOverlap = 30.dp

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
    weatherLine: String?,
    editing: Boolean,
    onShuffleBackdrop: () -> Unit,
    loadingBackdrop: Boolean,
    onSeedExtracted: (androidx.compose.ui.graphics.Color) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onAppMenu: (AppEntry) -> Unit,
    onReorder: (FolderConfig, List<AppEntry>) -> Unit,
    onFinishEditing: () -> Unit,
) {
    val pages = folders.size.coerceAtLeast(1)
    val cyclic = pages > 1

    // Бесконечная лента: стартуем из середины, чтобы круг работал в обе стороны.
    val startPage = remember(pages) { if (cyclic) (Int.MAX_VALUE / 2) - (Int.MAX_VALUE / 2) % pages else 0 }
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { if (cyclic) Int.MAX_VALUE else 1 },
    )
    val currentPage = (pagerState.currentPage - startPage).mod(pages)

    val columns = prefs.columns
    val iconSize = iconSizeFor(columns)
    val iconShape = prefs.iconShape
    val iconPack = prefs.iconPack

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Backdrop(
            backdrop = backdrop,
            clockSize = prefs.clockSize,
            twentyFour = prefs.clockTwentyFour,
            showDate = prefs.showDate,
            weatherLine = weatherLine,
            clockFont = prefs.clockFont,
            onShuffle = onShuffleBackdrop,
            loading = loadingBackdrop,
            onOpenSettings = onOpenSettings,
            onSeedExtracted = onSeedExtracted,
            modifier = Modifier
                .fillMaxWidth()
                .height(BackdropHeight),
        )

        Column(Modifier.fillMaxSize()) {

            Spacer(Modifier.height(BackdropHeight - SheetOverlap))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !editing,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val index = (page - startPage).mod(pages)
                    val config = folders.getOrNull(index) ?: return@HorizontalPager
                    val folder = remember(config, apps) { config.resolve(apps) }

                    Column(Modifier.fillMaxSize()) {
                        FolderHeader(
                            title = if (config.isAll) stringResource(R.string.all_apps) else folder.title,
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
                            showLabels = prefs.showLabels,
                            editing = editing,
                            onReorder = { onReorder(config, it) },
                            onClick = { repository.launch(it) },
                            onLongClick = onAppMenu,
                        )
                    }
                }
            }

            PageDots(pages = pages, current = currentPage)

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
