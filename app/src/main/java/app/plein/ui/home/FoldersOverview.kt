package app.plein.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import androidx.compose.ui.res.stringResource
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.FolderConfig
import app.plein.data.MonoMode
import app.plein.data.displayTitle
import app.plein.ui.icons.IconShape
import app.plein.ui.theme.MonoFont
import app.plein.ui.rememberHaptics

/**
 * Обзор папок по щипку.
 *
 * Страницы плиткой: в каждой первые четыре значка и счёт. Нажатие переносит
 * на страницу, а не открывает её отдельным экраном — обзор нужен, чтобы
 * прыгнуть, а не чтобы жить в нём.
 */
@Composable
fun FoldersOverview(
    folders: List<FolderConfig>,
    apps: List<AppEntry>,
    repository: AppRepository,
    iconShape: IconShape,
    iconPack: String,
    monoMode: MonoMode,
    current: Int,
    onPick: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val haptics = rememberHaptics()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClose)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.folders_overview),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 14.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(folders, key = { _, folder -> folder.id }) { index, config ->
                    val folder = config.resolve(apps)
                    val active = index == current
                    val scale by animateFloatAsState(
                        targetValue = if (active) 1f else 0.97f,
                        animationSpec = spring(dampingRatio = 0.5f),
                        label = "page",
                    )
                    Surface(
                        color = if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.92f)
                            .scale(scale)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable {
                                haptics.tick()
                                onPick(index)
                            },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = config.displayTitle(),
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = folder.apps.size.toString(),
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                            )
                            // Четыре значка достаточно, чтобы папку узнали в лицо.
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                folder.apps.take(4).forEach { entry ->
                                    AppIcon(
                                        entry = entry,
                                        repository = repository,
                                        size = 34.dp,
                                        iconShape = iconShape,
                                        iconPack = iconPack,
                                        monoMode = monoMode,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
