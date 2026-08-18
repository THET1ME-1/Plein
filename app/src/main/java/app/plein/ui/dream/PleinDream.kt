package app.plein.ui.dream

import android.os.Build
import android.service.dreams.DreamService
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.res.stringResource
import app.plein.R
import app.plein.data.Prefs
import app.plein.ui.theme.DisplayFont
import app.plein.ui.theme.MonoFont
import app.plein.ui.theme.PleinTheme
import app.plein.ui.theme.googleFontFamily
import app.plein.ui.theme.isDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Полоса ночи под часами.
 *
 * Отсчёт идёт от минуты, когда телефон поставили на зарядку, а не от условных
 * десяти вечера: лёг в два ночи — полоса начинается с нуля. Будильник дальше
 * шестнадцати часов это уже не сон, а завтрашние дела, и полосы не будет.
 */
object NightScale {

    /** Дольше этого ночь не бывает. */
    private const val LONGEST_HOURS = 16

    data class Scale(val progress: Float, val minutesLeft: Int)

    fun of(startedAt: Long, now: Long, alarmAt: Long?): Scale? {
        val alarm = alarmAt ?: return null
        val span = alarm - startedAt
        if (span <= 0 || span > LONGEST_HOURS * 3_600_000L) return null
        val done = ((now - startedAt).toFloat() / span).coerceIn(0f, 1f)
        val left = kotlin.math.ceil((alarm - now).coerceAtLeast(0L) / 60_000.0).toInt()
        return Scale(done, left)
    }
}

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
                // Тему берём настоящую: раньше сюда подставлялось «система
                // тёмная», и при системной теме заставка всегда была чёрной.
                val systemDark = (resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                val dark = prefs.themeMode.isDark(systemDark)
                PleinTheme(
                    dark = dark,
                    seed = androidx.compose.ui.graphics.Color(
                        app.plein.ui.theme.SeedChoice.of(
                            fromPhoto = prefs.seedFromPhoto,
                            photo = prefs.photoSeed,
                            manual = prefs.seedColor,
                        )
                    ),
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
    var now by remember { mutableStateOf(Date()) }
    val startedAt = remember { System.currentTimeMillis() }

    // Минуты обновляются сами: заставка живёт часами, а не секунды.
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            kotlinx.coroutines.delay(20_000)
        }
    }

    // Следующий будильник берём у системы, а не выдумываем распорядок.
    val alarmAt = remember(now) {
        val alarms = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
        runCatching { alarms?.nextAlarmClock?.triggerTime }.getOrNull()
    }
    val scale = NightScale.of(startedAt, now.time, alarmAt)

    // Ход крошечный и очень медленный: за полчаса четыре точки. Неподвижные
    // цифры за ночь выжигают след, а заметный дрейф читается как перекос.
    val drift = rememberInfiniteTransition(label = "drift")
    val shiftX by drift.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1_800_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "x",
    )
    val shiftY by drift.animateFloat(
        initialValue = 3f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(tween(1_500_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "y",
    )

    val clockFamily = if (prefs.clockFont.isEmpty()) DisplayFont else googleFontFamily(prefs.clockFont)
    val locale = app.plein.ui.rememberLocale()
    val time = SimpleDateFormat(if (prefs.clockTwentyFour) "HH:mm" else "h:mm", locale).format(now)
    val date = SimpleDateFormat("EEEE, d MMMM", locale).format(now)
    val left = scale?.let {
        stringResource(R.string.dream_left, it.minutesLeft / 60, it.minutesLeft % 60)
    }

    DreamContent(
        time = time,
        date = date,
        left = left,
        progress = scale?.progress,
        clockFamily = clockFamily,
        shiftX = shiftX,
        shiftY = shiftY,
    )
}

/**
 * Что видно на заставке.
 *
 * Ни кадра, ни заливки: заставка висит часами, а на OLED чёрное не светится
 * вовсе. Светится только то, что нужно прочитать спросонья — время, день и
 * сколько осталось спать.
 *
 * Всё стоит на одной вертикали и чуть выше середины экрана: ровно посередине
 * блок кажется проваленным вниз. Отдельно от службы: тут нет ни таймеров, ни
 * чтения системы, поэтому эту часть можно снять на снимок и сверять.
 */
@Composable
fun DreamContent(
    time: String,
    date: String,
    left: String?,
    progress: Float?,
    clockFamily: androidx.compose.ui.text.font.FontFamily?,
    shiftX: Float = 0f,
    shiftY: Float = 0f,
) {
    val night = MaterialTheme.colorScheme.surface.let { surface ->
        if (surface.luminance() > 0.5f) surface else Color.Black
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(night)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 48.dp)
                .graphicsLayer {
                    translationX = shiftX
                    translationY = shiftY
                },
        ) {
            Text(
                text = time,
                fontFamily = clockFamily,
                fontWeight = FontWeight.W200,
                fontSize = 88.sp,
                lineHeight = 92.sp,
                letterSpacing = (-4).sp,
                color = MaterialTheme.colorScheme.primary,
            )

            // Полоса и подпись держат общую ширину и стоят под цифрами
            // одним блоком. Дата и остаток идут одной строкой: двумя они
            // разъезжались по краям и на узком экране налезали друг на друга.
            val line = 236.dp
            progress?.let { done ->
                Box(
                    Modifier
                        .padding(top = 28.dp)
                        .width(line)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(done)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                    )
                }
            }

            // Дата и остаток идут двумя строками по той же оси. Одной
            // строкой они не помещались под цифры: «вторник, 18 августа»
            // с остатком просят больше места, чем занимает время, а в
            // немецком строка ещё длиннее.
            Text(
                text = date,
                fontFamily = MonoFont,
                fontSize = 10.5.sp,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = if (progress == null) 24.dp else 14.dp),
            )
            left?.let {
                Text(
                    text = it,
                    fontFamily = MonoFont,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
