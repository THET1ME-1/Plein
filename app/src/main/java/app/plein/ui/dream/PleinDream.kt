package app.plein.ui.dream

import android.os.Build
import android.service.dreams.DreamService
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.plein.data.Prefs
import app.plein.ui.theme.MonoFont
import app.plein.ui.theme.PleinTheme
import app.plein.ui.theme.googleFontFamily
import app.plein.ui.theme.isDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Заставка на подставке и зарядке.
 *
 * Экран блокировки стороннему лаунчеру закрыт, а заставка — его законный
 * сосед: система сама зовёт её, когда телефон стоит и скучает.
 *
 * `DreamService` не владелец жизненного цикла и не хранит состояние, поэтому
 * Compose внутри падает, пока ему не выдать своего владельца. Здесь он и
 * заводится — вместе с реестром сохранения и хранилищем моделей.
 */
class PleinDream : DreamService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val registry = LifecycleRegistry(this)
    private val stateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = stateController.savedStateRegistry

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        isInteractive = false
        isScreenBright = false

        stateController.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED

        val prefs = Prefs(this)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PleinDream)
            setViewTreeViewModelStoreOwner(this@PleinDream)
            setViewTreeSavedStateRegistryOwner(this@PleinDream)
            setContent {
                val dark = prefs.themeMode.isDark(true)
                PleinTheme(
                    dark = dark,
                    seed = androidx.compose.ui.graphics.Color(prefs.seedColor),
                    amoled = prefs.amoled,
                    interfaceFont = prefs.interfaceFont,
                ) {
                    DreamFace(prefs)
                }
            }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        registry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        registry.currentState = Lifecycle.State.CREATED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDetachedFromWindow()
    }
}

/**
 * Лицо заставки: кадр, часы, дата.
 *
 * Всё содержимое медленно дрейфует по экрану: неподвижные светлые цифры за
 * ночь выжигают след на OLED.
 */
@Composable
private fun DreamFace(prefs: Prefs) {
    val context = LocalContext.current
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        val saved = prefs.savedBackdrop()
        photo = withContext(Dispatchers.IO) {
            runCatching {
                saved?.file?.let { android.graphics.BitmapFactory.decodeFile(it.path) }
                    ?: context.assets.open("backdrop_forest.jpg").use {
                        android.graphics.BitmapFactory.decodeStream(it)
                    }
            }.getOrNull()?.asImageBitmap()
        }
    }

    // Минуты обновляются сами: заставка живёт часами, а не секунды.
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            kotlinx.coroutines.delay(20_000)
        }
    }

    val drift = rememberInfiniteTransition(label = "drift")
    val shiftX by drift.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(120_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "x",
    )
    val shiftY by drift.animateFloat(
        initialValue = 14f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(90_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "y",
    )

    val clockFamily = if (prefs.clockFont.isEmpty()) MonoFont else googleFontFamily(prefs.clockFont)
    val locale = app.plein.ui.rememberLocale()
    val time = SimpleDateFormat(if (prefs.clockTwentyFour) "HH:mm" else "h:mm a", locale).format(now)
    val date = SimpleDateFormat("EEEE, d MMMM", locale).format(now)

    DreamContent(
        time = time,
        date = date,
        photo = photo,
        clockFamily = clockFamily,
        shiftX = shiftX,
        shiftY = shiftY,
    )
}

/**
 * Что видно на заставке.
 *
 * Отдельно от службы: тут нет ни таймеров, ни чтения файлов, поэтому эту
 * часть можно снять на снимок и сверять при каждой правке.
 */
@Composable
fun DreamContent(
    time: String,
    date: String,
    photo: ImageBitmap?,
    clockFamily: androidx.compose.ui.text.font.FontFamily?,
    shiftX: Float = 0f,
    shiftY: Float = 0f,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        photo?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.5f },
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.5f),
                        1f to Color.Black.copy(alpha = 0.75f),
                    )
                )
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = shiftX
                    translationY = shiftY
                }
                .padding(24.dp),
        ) {
            Text(
                text = time,
                fontFamily = clockFamily,
                fontSize = 72.sp,
                lineHeight = 76.sp,
                letterSpacing = (-2).sp,
                color = Color.White,
            )
            Text(
                text = date,
                fontFamily = MonoFont,
                fontSize = 12.sp,
                letterSpacing = 1.4.sp,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
