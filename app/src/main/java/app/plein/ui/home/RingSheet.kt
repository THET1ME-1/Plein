package app.plein.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plein.R
import app.plein.data.AppEntry
import app.plein.data.AppRepository
import app.plein.data.MonoMode
import app.plein.ui.icons.IconShape
import app.plein.ui.theme.MonoFont

/**
 * Раскрытая круговая папка.
 *
 * Кольцо держит шесть частых, здесь лежит весь состав — то, что за середину
 * и уехало. Значки те же, что на сетке: своя форма, свой пак, свой монохром.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingSheet(
    title: String,
    apps: List<AppEntry>,
    repository: AppRepository,
    columns: Int,
    iconSize: androidx.compose.ui.unit.Dp,
    iconShape: IconShape,
    iconPack: String,
    monoMode: MonoMode,
    showLabels: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listHeight = (LocalConfiguration.current.screenHeightDp * 0.5f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
            ) {
                Text(
                    text = title.ifBlank { stringResource(R.string.ring_default) },
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${apps.size}",
                    fontFamily = MonoFont,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }

            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.ring_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
                )
            } else {
                AppsGrid(
                    apps = apps,
                    repository = repository,
                    columns = columns,
                    iconSize = iconSize,
                    iconShape = iconShape,
                    iconPack = iconPack,
                    monoMode = monoMode,
                    showLabels = showLabels,
                    editing = false,
                    onReorder = {},
                    onClick = onLaunch,
                    onLongClick = onLongClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(listHeight)
                        .padding(bottom = 12.dp),
                )
            }
        }
    }
}
