package app.plein.ui.home

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.Backdrop
import app.plein.data.PhotoPalette
import app.plein.ui.theme.EmphasizedDecelerate
import app.plein.ui.theme.MonoFont
import app.plein.ui.theme.googleFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Верхняя зона: кадр, затемнение под текст, часы и подпись автора.
 * Размеры из макета: сцена 244, часы от низа 54, подпись от низа 40.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Backdrop(
    backdrop: Backdrop,
    clockSize: String,
    twentyFour: Boolean,
    showDate: Boolean,
    weatherTemp: String?,
    weatherCode: Int,
    onWeatherClick: () -> Unit = {},
    collapse: Float = 0f,
    pull: Float = 0f,
    clockFont: String,
    onShuffle: () -> Unit,
    loading: Boolean = false,
    onOpenSettings: () -> Unit,
    onSeedExtracted: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var photo by remember(backdrop.key) { mutableStateOf<ImageBitmap?>(null) }

    // Фигура заготовлена заранее: она крутится знаком загрузки, ею же
    // растекается пришедший кадр, и только после этого берётся следующая.
    // Иначе человек тянул одну форму, а на экран выезжала другая.
    var morph by remember { mutableStateOf(MorphReveal.randomMorph()) }
    val reveal = remember(backdrop.key) { Animatable(0f) }

    LaunchedEffect(backdrop.key) {
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                // Полное разрешение: уменьшение вдвое давало мыло на большом кадре.
                val options = android.graphics.BitmapFactory.Options()
                backdrop.file?.let { BitmapFactory.decodeFile(it.path, options) }
                    ?: backdrop.asset?.let { asset ->
                        context.assets.open(asset).use { BitmapFactory.decodeStream(it, null, options) }
                    }
            }.getOrNull()
        } ?: return@LaunchedEffect

        photo = decoded.asImageBitmap()
        val seed = withContext(Dispatchers.Default) { PhotoPalette.seedFrom(decoded) }
        onSeedExtracted(seed)
        reveal.animateTo(1f, tween(650, easing = EmphasizedDecelerate))
        morph = MorphReveal.randomMorph()
    }

    val time = remember(twentyFour) {
        SimpleDateFormat(if (twentyFour) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date())
    }
    val clockFontSize = when (clockSize) {
        "s" -> 30.sp
        "l" -> 52.sp
        "xl" -> 64.sp
        else -> 40.sp
    }
    val clockFamily = if (clockFont.isEmpty()) MonoFont else googleFontFamily(clockFont)
    // Дата берёт язык интерфейса: раньше стояла русская локаль, и англичанин
    // видел «ПТ, 8 АВГ».
    val locale = Locale.getDefault()
    val date = remember(locale) {
        SimpleDateFormat("EEE, d MMM", locale).format(Date()).uppercase(locale)
    }

    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onOpenSettings,
            )
    ) {
        photo?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        // Клип нужен только пока фигура растекается.
                        if (reveal.value < 1f) Modifier.clip(MorphShape(morph, reveal.value)) else Modifier
                    ),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.34f to Color.Black.copy(alpha = 0.12f),
                        1f to Color.Black.copy(alpha = 0.62f),
                    )
                )
        )

        if (pull > 0.02f || loading) {
            // Фигура растёт под пальцем и доворачивается; дотянул до полного —
            // она крутится, пока едет кадр, и им же растекается по экрану.
            val reach = pull.coerceAtMost(1f)
            MorphSpinner(
                morph = morph,
                size = if (loading) 46.dp else (26 + 22 * reach).dp,
                color = Color.White.copy(alpha = if (loading) 0.85f else 0.35f + 0.5f * reach),
                spinning = loading,
                turn = reach * 140f,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 46.dp, end = 12.dp),
        ) {
            GlassButton(onClick = onShuffle, description = stringResource(R.string.another_backdrop)) {
                if (loading) {
                    // Та же фигура, что и на кадре: одна пластика на весь экран.
                    MorphSpinner(morph = morph, size = 18.dp, color = Color.White, spinning = true)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            GlassButton(onClick = onOpenSettings, description = stringResource(R.string.settings)) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 54.dp)
                .graphicsLayer {
                    // Часы едут вверх и мельчают: к концу хода остаётся строка.
                    val scale = 1f - collapse * 0.42f
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                    translationY = -collapse * 26f
                }
        ) {
            Text(
                text = time,
                fontFamily = clockFamily,
                fontSize = clockFontSize,
                lineHeight = clockFontSize,
                letterSpacing = (-1.2).sp,
                color = Color.White,
            )
            if (showDate || weatherTemp != null) Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(CircleShape)
                    .combinedClickable(onClick = onWeatherClick)
                    .padding(vertical = 2.dp),
            ) {
                if (weatherTemp != null) {
                    Icon(
                        weatherIcon(weatherCode),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(13.dp)
                            .padding(end = 1.dp),
                    )
                }
                Text(
                    text = listOfNotNull(date.takeIf { showDate }, weatherTemp).joinToString(" · "),
                fontFamily = MonoFont,
                fontSize = 10.5.sp,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.W400,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }

        Text(
            text = backdrop.credit,
            fontFamily = MonoFont,
            fontSize = 9.5.sp,
            letterSpacing = 0.4.sp,
            color = Color.White.copy(alpha = 0.82f),
            textAlign = TextAlign.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 40.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassButton(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .combinedClickable(onClick = onClick, onClickLabel = description),
        contentAlignment = Alignment.Center,
    ) { content() }
}
