package app.plein.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.FontCatalog
import app.plein.ui.theme.googleFontFamily
import app.plein.ui.theme.googleFontsAvailable
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Выбор шрифта.
 *
 * Каталог Google Fonts, поиск по названию. Файлы не качаем: семейство
 * приезжает через провайдер Play Services по имени, и каждая строка показана
 * этим же шрифтом — с образцом под названием, чтобы характер и поддержку
 * кириллицы было видно, не выходя из списка.
 *
 * Провайдера на телефоне может не быть вовсе. Тогда Compose молча подставляет
 * запасной шрифт, и список выглядит как девяносто девять одинаковых строк —
 * поэтому о таком телефоне лист говорит прямо, вместо списка-обманки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontPickerSheet(
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val families = remember(query) { FontCatalog.search(query) }
    val context = LocalContext.current
    val fontsAvailable = remember(context) { googleFontsAvailable(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                text = stringResource(R.string.interface_font),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, bottom = 12.dp),
            )
            PlainSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.font_search_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            if (!fontsAvailable) {
                Text(
                    text = stringResource(R.string.font_provider_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp),
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                // Ростом от экрана: на маленьком телефоне жёсткие 420 упирались
                // в системные кнопки.
                modifier = Modifier.height((LocalConfiguration.current.screenHeightDp * 0.52f).dp),
            ) {
                item {
                    FontRow(
                        family = "",
                        title = "Unbounded · Onest",
                        selected = current.isEmpty(),
                        available = true,
                        onClick = { onPick(""); onDismiss() },
                    )
                }
                items(families, key = { it }) { family ->
                    FontRow(
                        family = family,
                        title = family,
                        selected = family == current,
                        available = fontsAvailable,
                        onClick = { onPick(family); onDismiss() },
                    )
                }
            }
        }
    }
}

/** Что успел сделать провайдер с этим шрифтом. */
internal enum class Preview { Loading, Ready, Failed }

@Composable
internal fun FontRow(
    family: String,
    title: String,
    selected: Boolean,
    available: Boolean,
    onClick: () -> Unit,
) {
    val resolver = LocalFontFamilyResolver.current
    val fontFamily = if (family.isEmpty()) null else googleFontFamily(family)
    var preview by remember(family, available) {
        mutableStateOf(if (family.isEmpty() || !available) Preview.Ready else Preview.Loading)
    }

    // Шрифт заказываем сами, а не ждём, пока его попросит Text: иначе неудачу
    // видно только по тому, что строка не изменилась, и «загружается» не
    // отличить от «не приедет никогда». Потолок по времени нужен, чтобы строка
    // не осталась в загрузке навсегда.
    LaunchedEffect(family, available) {
        if (fontFamily == null || !available) return@LaunchedEffect
        preview = withTimeoutOrNull(FontLoadTimeoutMs) {
            runCatching { resolver.preload(fontFamily) }.fold({ Preview.Ready }, { Preview.Failed })
        } ?: Preview.Failed
    }

    val ready = preview == Preview.Ready
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                // Пока шрифт едет, имя набрано своим: подменять его на полпути
                // значит дёргать список при каждой загрузке.
                fontFamily = if (ready) fontFamily else null,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp),
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            when {
                family.isEmpty() -> Unit
                preview == Preview.Failed -> Text(
                    text = stringResource(R.string.font_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                else -> Text(
                    text = stringResource(R.string.font_sample),
                    fontFamily = if (ready) fontFamily else null,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    // Образец гаснет, пока шрифт едет: строка не прыгает, но и
                    // не притворяется загруженной.
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (ready) 1f else 0.4f
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Дольше этого провайдер не отвечает уже никогда. */
private const val FontLoadTimeoutMs = 8_000L
