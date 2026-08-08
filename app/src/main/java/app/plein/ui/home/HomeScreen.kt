package app.plein.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.Folder
import app.plein.data.Prefs
import app.plein.ui.theme.Emphasized
import app.plein.ui.theme.MonoFont
import app.plein.ui.theme.SheetCorner
import androidx.compose.animation.core.tween

private val BackdropHeight = 244.dp
private val SheetOverlap = 30.dp

/**
 * Главный экран.
 *
 * Кадр сверху, лист с приложениями наезжает на него скруглением 30,
 * точки и поиск закреплены внизу: поиск ищет по телефону, а не по папке,
 * поэтому он не уезжает вместе со страницами.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    folders: List<Folder>,
    repository: AppRepository,
    prefs: Prefs,
    backdropAuthor: String,
    onShuffleBackdrop: () -> Unit,
    onSeedExtracted: (androidx.compose.ui.graphics.Color) -> Unit,
    onOpenSearch: () -> Unit,
    onAppMenu: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { folders.size.coerceAtLeast(1) })
    val columns = prefs.columns
    val iconSize = iconSizeFor(columns)
    val rowGap = if (columns >= 5) 12.dp else 20.dp
    val shape = remember(prefs.iconShape) { prefs.iconShape.shape() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Backdrop(
            author = backdropAuthor,
            onShuffle = onShuffleBackdrop,
            onLongPress = onOpenSettings,
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
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val folder = folders.getOrNull(page) ?: return@HorizontalPager
                    Column(Modifier.fillMaxSize()) {
                        FolderHeader(
                            title = folder.title,
                            page = page + 1,
                            pages = folders.size,
                            count = folder.apps.size,
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(rowGap),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(folder.apps, key = { it.key }) { entry ->
                                AppCell(
                                    entry = entry,
                                    repository = repository,
                                    iconSize = iconSize,
                                    shape = shape,
                                    showLabel = prefs.showLabels,
                                    onClick = { repository.launch(entry) },
                                    onLongClick = { onAppMenu(entry) },
                                )
                            }
                        }
                    }
                }
            }

            PageDots(
                pages = folders.size,
                current = pagerState.currentPage,
            )

            SearchPill(onClick = onOpenSearch)
        }
    }
}

private fun iconSizeFor(columns: Int): Dp = when (columns) {
    3 -> 82.dp
    4 -> 61.dp
    5 -> 48.dp
    else -> 40.dp
}

@Composable
private fun FolderHeader(title: String, page: Int, pages: Int, count: Int) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(
    entry: AppEntry,
    repository: AppRepository,
    iconSize: Dp,
    shape: Shape,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            // Скругление ячейки съедало края подписи: она стоит у самого низа,
            // поэтому радиус мельче, а под текстом остаётся отступ.
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(bottom = 6.dp),
    ) {
        AppIcon(entry = entry, repository = repository, size = iconSize, shape = shape)
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            // Box держит ширину ячейки, иначе длинное имя вылезает за колонку
            // и обрезается клипом с обеих сторон вместо честного многоточия.
            Box(
                modifier = Modifier.width(iconSize + 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Выравнивание держит Box: при textAlign = Center длинное имя
                // рисуется от центра и первая буква уходит под клип ячейки.
                Text(
                    text = entry.title,
                    fontSize = 10.5.sp,
                    lineHeight = 12.sp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                text = "Поиск по телефону",
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
