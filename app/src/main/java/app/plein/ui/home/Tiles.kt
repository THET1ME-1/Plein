package app.plein.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import app.plein.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.ui.theme.MonoFont

/**
 * Плитки лаунчера.
 *
 * Свои, а не системные виджеты: рисуются в наших токенах, знают форму и цвет
 * темы, ничего не тянут через RemoteViews и не приносят чужой отрисовки в нашу
 * прокрутку. Системные виджеты придут отдельным слоем, эти останутся первыми.
 */
object Tiles {

    const val CLOCK = "clock"
    const val WEATHER = "weather"
    const val BATTERY = "battery"
    const val CALENDAR = "calendar"
    const val NOTE = "note"
    const val MEDIA = "media"

    /** Размер по умолчанию в клетках: ширина к высоте. */
    fun sizeOf(kind: String): Pair<Int, Int> = when (kind) {
        CLOCK -> 2 to 2
        WEATHER -> 2 to 2
        BATTERY -> 2 to 1
        CALENDAR -> 4 to 1
        NOTE -> 4 to 2
        MEDIA -> 4 to 2
        else -> 2 to 2
    }

    val all = listOf(CLOCK, WEATHER, BATTERY, CALENDAR, NOTE, MEDIA)
}

/** Общая оболочка плитки: заливка, форма, отступ от соседей. */
@Composable
fun TileSurface(
    modifier: Modifier = Modifier,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable () -> Unit,
) {
    // Нажатия плитка не ловит: и короткое, и долгое разбирает страница. Иначе
    // внутренний обработчик съедал удержание, и вместо захвата открывалось
    // приложение.
    // Внешние отступы задаёт страница: они считаются от размера значка, чтобы
    // плитка встала ровно вровень с иконками, а не по краю клетки.
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(container)
            .padding(14.dp),
    ) { content() }
}

@Composable
fun ClockTile(time: String, date: String) {
    TileSurface(container = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
            )
            Text(
                text = date,
                fontFamily = MonoFont,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun WeatherTile(temperature: String?, code: Int, place: String) {
    TileSurface {
        Column(Modifier.fillMaxSize()) {
            Icon(
                weatherIcon(code),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = temperature ?: "—",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = place,
                        fontFamily = MonoFont,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryTile(percent: Int, charging: Boolean) {
    TileSurface {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (charging) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.BatteryFull,
                    contentDescription = null,
                    tint = if (charging) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
fun CalendarTile(title: String, time: String) {
    TileSurface {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            Text(
                text = time,
                fontFamily = MonoFont,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun NoteTile(text: String) {
    TileSurface(container = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxSize()) {
            Icon(
                Icons.Rounded.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Плитка «сейчас играет».
 *
 * Заливку берём из обложки: тот же приём, что и с кадром на домашнем экране,
 * поэтому плитка меняет цвет вместе с альбомом и не выпадает из палитры.
 * Обложка кладётся плашмя, без тени и обводки — как всё остальное здесь.
 *
 * Размер плитка узнаёт сама: в две клетки высотой встаёт обложка с подписью и
 * тремя кнопками, в одну — полоса с одной кнопкой, в квадрат — обложка и
 * кнопка поверх.
 */
@Composable
fun MediaTile(
    title: String?,
    artist: String,
    art: android.graphics.Bitmap?,
    playing: Boolean,
    allowed: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    // Тон из обложки приглушаем: плитка должна отличаться от соседей, а не
    // спорить с ними за внимание.
    val container = remember(art) {
        art?.let { app.plein.data.PhotoPalette.seedFrom(it).copy(alpha = 0.3f).compositeOver(surface) } ?: surface
    }
    val ink = MaterialTheme.colorScheme.onSurface

    TileSurface(container = container) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tall = this.maxHeight > 96.dp
            val wide = this.maxWidth > 190.dp
            val squareArt = this.maxWidth - 28.dp

            when {
                !allowed -> MediaHint(stringResource(R.string.media_allow), ink)
                title == null -> MediaHint(stringResource(R.string.media_silence), ink)

                // Обложка держит всю высоту плитки: квадрат в две клетки
                // читается как обложка пластинки, а маленький значок сбоку —
                // как строчка списка.
                wide -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                    MediaArt(art, if (tall) this@BoxWithConstraints.maxHeight else 42.dp, ink)
                    Column(
                        verticalArrangement = if (tall) Arrangement.SpaceBetween else Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 14.dp)
                    ) {
                        Column(Modifier.padding(top = if (tall) 4.dp else 0.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.W500,
                                color = ink,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 19.sp,
                            )
                        if (artist.isNotEmpty()) {
                            Text(
                                text = artist,
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                letterSpacing = 1.1.sp,
                                color = ink.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        }
                        if (tall) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                MediaButton(Icons.Rounded.SkipPrevious, 20.dp, ink, onPrevious)
                                MediaButton(
                                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    28.dp, ink, onToggle,
                                )
                                MediaButton(Icons.Rounded.SkipNext, 20.dp, ink, onNext)
                            }
                        }
                    }
                    if (!tall) {
                        MediaButton(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            24.dp, ink, onToggle,
                        )
                    }
                }

                // Квадрат: обложка во всю плитку, кнопка поверх правого нижнего угла.
                else -> Box(Modifier.fillMaxSize()) {
                    MediaArt(art, squareArt, ink)
                    Box(Modifier.align(Alignment.BottomEnd)) {
                        MediaButton(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            24.dp, ink, onToggle,
                        )
                    }
                }
            }
        }
    }
}

/** Обложка или её место, пока картинки нет. */
@Composable
private fun MediaArt(art: android.graphics.Bitmap?, size: Dp, ink: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(if (size > 90.dp) 20.dp else 14.dp))
            .background(ink.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = ink.copy(alpha = 0.5f),
                modifier = Modifier.size(size / 2.4f),
            )
        }
    }
}

@Composable
private fun MediaButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: Dp,
    ink: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size + 16.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(size))
    }
}

/** Строка вместо плеера: нет доступа или все молчат. */
@Composable
private fun MediaHint(text: String, ink: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = ink.copy(alpha = 0.55f),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
            color = ink.copy(alpha = 0.75f),
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
