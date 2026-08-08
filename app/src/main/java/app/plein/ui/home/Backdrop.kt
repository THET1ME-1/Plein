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
    onShuffle: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeedExtracted: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var photo by remember(backdrop.asset) { mutableStateOf<ImageBitmap?>(null) }

    // Каждая смена кадра рисует свою фигуру и растекается ею в прямоугольник.
    val morph = remember(backdrop.asset) { MorphReveal.randomMorph() }
    val reveal = remember(backdrop.asset) { Animatable(0f) }

    LaunchedEffect(backdrop.asset) {
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(backdrop.asset).use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        } ?: return@LaunchedEffect

        photo = decoded.asImageBitmap()
        val seed = withContext(Dispatchers.Default) { PhotoPalette.seedFrom(decoded) }
        onSeedExtracted(seed)
        reveal.animateTo(1f, tween(650, easing = EmphasizedDecelerate))
    }

    val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    val date = remember {
        SimpleDateFormat("EEE, d MMM", Locale("ru")).format(Date()).uppercase(Locale("ru"))
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
                    .clip(MorphShape(morph, reveal.value)),
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 46.dp, end = 12.dp),
        ) {
            GlassButton(onClick = onShuffle, description = stringResource(R.string.another_backdrop)) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            GlassButton(onClick = onOpenSettings, description = stringResource(R.string.settings)) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 54.dp)
        ) {
            Text(
                text = time,
                fontFamily = MonoFont,
                fontSize = 40.sp,
                lineHeight = 40.sp,
                letterSpacing = (-1.2).sp,
                color = Color.White,
            )
            Text(
                text = "$date · +19°",
                fontFamily = MonoFont,
                fontSize = 10.5.sp,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.W400,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Text(
            text = stringResource(R.string.photo_credit, backdrop.author),
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
