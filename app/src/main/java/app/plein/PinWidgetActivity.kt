package app.plein

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import app.plein.data.LayoutStore
import app.plein.data.Prefs
import app.plein.data.Widgets
import app.plein.ui.theme.PleinTheme
import app.plein.ui.theme.SeedChoice
import app.plein.ui.theme.isDark
import androidx.compose.ui.graphics.Color

/**
 * Закрепление виджета по просьбе самого приложения.
 *
 * Без этой активности система отвечает приложениям, что лаунчер виджеты не
 * поддерживает: `isRequestPinAppWidgetSupported()` смотрит именно на то, есть
 * ли у домашнего экрана приёмник `CONFIRM_PIN_APPWIDGET`. Поэтому «добавить
 * виджет» изнутри чужого приложения не работало вовсе — списком в лаунчере
 * виджет поставить было можно, а его собственной кнопкой нет.
 *
 * Порядок: спрашиваем человека, выдаём номер, отдаём его системе в `accept`,
 * при надобности открываем экран настройки виджета и только потом кладём его
 * на страницу, где человек был последний раз.
 */
class PinWidgetActivity : ComponentActivity() {

    private lateinit var widgets: Widgets
    private lateinit var layoutStore: LayoutStore
    private lateinit var prefs: Prefs

    private var request: LauncherApps.PinItemRequest? = null
    private var info: AppWidgetProviderInfo? = null
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgets = Widgets(this)
        layoutStore = LayoutStore(this)
        prefs = Prefs(this)

        val pin = intent?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableExtra(
                    LauncherApps.EXTRA_PIN_ITEM_REQUEST,
                    LauncherApps.PinItemRequest::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableExtra<LauncherApps.PinItemRequest>(
                    LauncherApps.EXTRA_PIN_ITEM_REQUEST
                )
            }
        }

        // Запрос живёт недолго и умирает вместе с приложением, которое его
        // послало: проверяем годность до того, как что-то обещать человеку.
        val provider = pin?.takeIf { it.isValid }
            ?.takeIf { it.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET }
            ?.getAppWidgetProviderInfo(this)

        if (pin == null || provider == null) {
            finish()
            return
        }

        request = pin
        info = provider

        setContent {
            val dark = prefs.themeMode.isDark(
                systemDark = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            )
            PleinTheme(
                dark = dark,
                seed = Color(
                    SeedChoice.of(prefs.seedFromPhoto, prefs.photoSeed, prefs.seedColor)
                ),
                dynamicColor = prefs.dynamicColor,
                amoled = prefs.amoled,
                vibrancy = prefs.vibrancy,
                interfaceFont = prefs.interfaceFont,
            ) {
                PinSheet(
                    label = provider.loadLabel(packageManager).orEmpty(),
                    preview = remember {
                        runCatching { provider.loadPreviewImage(this@PinWidgetActivity, 0) }
                            .getOrNull()
                            ?: runCatching { provider.loadIcon(this@PinWidgetActivity, 0) }
                                .getOrNull()
                    },
                    onConfirm = { accept() },
                    onDismiss = { finish() },
                )
            }
        }
    }

    /**
     * Принять запрос.
     *
     * Номер выдаёт наш хост, и система ждёт его в `accept`: без него виджет
     * привяжется к чужому номеру, а рисовать его будет некому.
     */
    private fun accept() {
        val pin = request ?: return finish()
        val provider = info ?: return finish()

        widgetId = widgets.allocateId()
        val accepted = runCatching {
            pin.accept(Bundle().apply { putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId) })
        }.getOrDefault(false)

        if (!accepted) {
            drop()
            return
        }

        // Виджету со своим экраном настройки без него нечего показывать:
        // открываем его сразу, а на страницу кладём только настроенный.
        if (widgets.needsConfigure(provider)) {
            val started = widgets.startConfigure(this, widgetId, CONFIGURE_REQUEST)
            if (!started) place()
        } else {
            place()
        }
    }

    @Deprecated("Экран настройки виджета открывает хост, а он отвечает старым способом")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CONFIGURE_REQUEST) return
        // Настройку прервали — номер отпускаем: иначе он повиснет за нами, а
        // виджета на экране всё равно нет.
        if (resultCode == RESULT_OK) place() else drop()
    }

    private fun place() {
        val provider = info ?: return finish()
        val columns = prefs.columns
        val cellWidth = cellWidthDp(columns)
        val cellHeight = app.plein.ui.home.CellMetrics.resolve(
            custom = prefs.rowHeight,
            columns = columns,
            showLabels = prefs.showLabels,
        ).value.toInt()
        val (width, height) = widgets.cellsFor(provider, cellWidth, cellHeight)

        layoutStore.addWidget(folderId(), widgetId, width, height, columns)
        finish()
    }

    private fun drop() {
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) widgets.release(widgetId)
        finish()
    }

    /** Куда класть: страница, открытая последней, иначе первая по счёту. */
    private fun folderId(): String {
        val last = prefs.lastFolder
        if (last.isNotEmpty()) return last
        return app.plein.data.FolderStore(this).folders.firstOrNull()?.id.orEmpty()
    }

    private companion object {
        const val CONFIGURE_REQUEST = 0x504D
    }

    private fun cellWidthDp(columns: Int): Int {
        val screen = resources.configuration.screenWidthDp
        // Те же поля, что у страницы: по 20 точек с каждой стороны.
        return ((screen - 40) / columns).coerceAtLeast(1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun PinSheet(
    label: String,
    preview: android.graphics.drawable.Drawable?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_pin_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            preview?.let { drawable ->
                Box(
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = remember(drawable) { drawable.toBitmap().asImageBitmap() },
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                TextButton(onClick = onDismiss, shape = CircleShape) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = onConfirm, shape = CircleShape) {
                    Text(stringResource(R.string.widget_pin_add))
                }
            }
        }
    }
}
