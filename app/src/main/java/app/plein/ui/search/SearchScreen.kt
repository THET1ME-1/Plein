package app.plein.ui.search

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Tune
import app.plein.R
import app.plein.data.AppEntry
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import app.plein.search.Calculator
import app.plein.search.Contact
import app.plein.search.ContactSearch
import app.plein.search.Contacts
import app.plein.search.Stores
import app.plein.data.AppRepository
import app.plein.search.Converter
import app.plein.search.Currency
import app.plein.search.SystemSettings
import app.plein.ui.home.AppIcon
import app.plein.ui.theme.MonoFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Поиск по телефону: приложения, счёт выражений и запросы наружу.
 * Ищет по всему, а не по текущей папке, поэтому и живёт отдельным экраном.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    apps: List<AppEntry>,
    repository: AppRepository,
    iconShape: app.plein.ui.icons.IconShape,
    webProvider: String = "google",
    initialQuery: String = "",
    searchContacts: Boolean = true,
    contactsAsked: Boolean = false,
    onContactsAsked: () -> Unit = {},
    onVoice: (() -> Unit)? = null,
    onAppMenu: (AppEntry) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val searchLocale = app.plein.ui.rememberLocale()
    var query by remember { mutableStateOf(initialQuery) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    // Порядок держит AppRanker: совпадение по буквам плюс привычка. Пустой
    // запрос показывает то, что открывают в этот час чаще всего.
    val stats = repository.stats
    val ranked = remember(query, apps) {
        val hour = stats.nowHour()
        val total = stats.total()
        apps.map { entry ->
            entry to app.plein.search.AppRanker.score(
                title = entry.title,
                query = query,
                launches = stats.launches(entry.key),
                launchesAtHour = stats.launchesAt(entry.key, hour),
                totalLaunches = total,
            )
        }
            .filter { it.second > 0.0 }
            .sortedWith(compareByDescending<Pair<AppEntry, Double>> { it.second }.thenBy { it.first.title })
            .take(if (query.isBlank()) FREQUENT else 24)
            .map { it.first }
    }

    // Пустая строка показывает ряд значков, набранная — список строками:
    // одно и то же в двух видах на экране не нужно.
    val frequent = if (query.isBlank()) ranked else emptyList()
    val matches = if (query.isBlank()) emptyList() else ranked

    // Телефонная книга. Читаем в фоне: у провайдера свой курсор и свой диск.
    val contacts = remember { ContactSearch(context) }
    var people by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var mayReadContacts by remember { mutableStateOf(Contacts.granted(context)) }
    val askContacts = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> mayReadContacts = granted }

    LaunchedEffect(query, searchContacts, mayReadContacts) {
        if (!searchContacts || query.isBlank()) {
            people = emptyList()
            return@LaunchedEffect
        }
        if (!mayReadContacts) {
            // Спрашиваем в момент первого поиска и ровно один раз: отказ
            // должен запоминаться, иначе запрос лезет на каждую букву.
            if (!contactsAsked) {
                onContactsAsked()
                askContacts.launch(Manifest.permission.READ_CONTACTS)
            }
            return@LaunchedEffect
        }
        people = withContext(Dispatchers.IO) { contacts.find(query) }
    }

    // Магазины показываем, только когда на телефоне ничего не нашлось.
    val stores = remember {
        val packages = context.packageManager
        Stores.available(
            Stores.KNOWN.filter { runCatching { packages.getPackageInfo(it, 0) }.isSuccess }.toSet()
        )
    }
    val calculated = remember(query) { Calculator.evaluate(query)?.let(Calculator::format) }

    // Перевод величин считается на месте, курс валют идёт в сеть и живёт
    // сутки в кэше: без сети показываем вчерашний и подписываем дату.
    val converted = remember(query) { Converter.convert(query) }
    val currency = remember { Currency(context) }
    var money by remember(query) { mutableStateOf<Pair<Double, Currency.Rate>?>(null) }
    LaunchedEffect(query) {
        money = null
        val parsed = currency.parse(query) ?: return@LaunchedEffect
        val rate = currency.rate(parsed.second, parsed.third) ?: return@LaunchedEffect
        money = parsed.first * rate.value to rate
    }

    val settingsFound = remember(query) { SystemSettings.search(context, query) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                modifier = Modifier.weight(1f),
            ) {
                calculated?.let { result ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            ) {
                                Text(
                                    text = "$query =",
                                    fontFamily = MonoFont,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }

                converted?.let { result ->
                    item {
                        ResultCard(
                            left = result.source,
                            right = "${Converter.format(result.value)} ${result.unit}",
                        )
                    }
                }

                money?.let { (value, rate) ->
                    item {
                        ResultCard(
                            left = query,
                            right = "${Converter.format(value)} ${rate.code}",
                            note = if (rate.updated > 0) {
                                stringResource(
                                    R.string.rate_stale,
                                    java.text.SimpleDateFormat("d MMM", searchLocale)
                                        .format(java.util.Date(rate.updated)),
                                )
                            } else {
                                null
                            },
                        )
                    }
                }

                if (frequent.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.search_section_frequent)) }
                    item {
                        FrequentRow(
                            apps = frequent,
                            repository = repository,
                            iconShape = iconShape,
                            onOpen = { entry ->
                                repository.launch(entry)
                                onClose()
                            },
                            onMenu = onAppMenu,
                        )
                    }
                }

                if (settingsFound.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.search_section_settings)) }
                    items(settingsFound, key = { it.action }) { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SystemSettings.open(context, entry)
                                    onClose()
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                text = stringResource(entry.titleRes),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 14.dp),
                            )
                        }
                    }
                }

                if (matches.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.search_section_apps)) }
                    items(matches, key = { it.key }) { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        repository.launch(entry)
                                        onClose()
                                    },
                                    onLongClick = { onAppMenu(entry) },
                                )
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            AppIcon(entry = entry, repository = repository, size = 44.dp, iconShape = iconShape)
                            Column(Modifier.padding(start = 14.dp)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = entry.component.packageName,
                                    fontFamily = MonoFont,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                if (people.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.search_section_contacts)) }
                    items(people, key = { it.id }) { person ->
                        ContactRow(
                            person = person,
                            onOpen = {
                                val card = Uri.withAppendedPath(
                                    android.provider.ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                                    person.lookup,
                                )
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, card)) }
                                onClose()
                            },
                            onCall = {
                                val call = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                runCatching { context.startActivity(call) }
                                onClose()
                            },
                            onWrite = {
                                val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${person.phone}"))
                                runCatching { context.startActivity(sms) }
                                onClose()
                            },
                        )
                    }
                }

                // Ничего не нашлось на телефоне — значит приложения нет, и
                // разговор переходит в магазин.
                if (query.isNotBlank() && matches.isEmpty()) {
                    item { SectionLabel(stringResource(R.string.search_section_install)) }
                    items(stores, key = { it }) { store ->
                        val name = when (store) {
                            Stores.PLAY -> "Google Play"
                            Stores.RUSTORE -> "RuStore"
                            else -> "F-Droid"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val language = searchLocale.language
                                    val direct = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(Stores.url(store, query, language)),
                                    )
                                    // Схему market:// подхватывать может быть
                                    // некому: тогда тот же поиск в браузере.
                                    if (runCatching { context.startActivity(direct) }.isFailure) {
                                        val web = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(Stores.webUrl(store, query, language)),
                                        )
                                        runCatching { context.startActivity(web) }
                                    }
                                    onClose()
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.search_store, name),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 14.dp),
                            )
                        }
                    }
                }

                if (query.isNotBlank()) {
                    item { SectionLabel(stringResource(R.string.search_section_outside)) }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(app.plein.search.WebSearch.url(webProvider, query)),
                                    )
                                    runCatching { context.startActivity(intent) }
                                    onClose()
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.search_web, query),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 14.dp),
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                    .height(58.dp),
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
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focus),
                        )
                    }
                    // Микрофон рядом с крестиком: в поиске он нужнее всего,
                    // когда набирать долго. Нет распознавания — нет и кнопки.
                    onVoice?.let { voice ->
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(onClick = voice),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Mic,
                                contentDescription = stringResource(R.string.voice_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { if (query.isEmpty()) onClose() else query = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Сколько значков стоит в ряду частых. Шестой уже не помещается в строку. */
private const val FREQUENT = 5

/**
 * Ряд частых приложений.
 *
 * Показывается, пока строка пуста. Порядок берётся из того же `AppRanker`,
 * что и поиск: утром сверху почта, вечером плеер.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FrequentRow(
    apps: List<AppEntry>,
    repository: AppRepository,
    iconShape: app.plein.ui.icons.IconShape,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        apps.forEach { entry ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .combinedClickable(
                        onClick = { onOpen(entry) },
                        onLongClick = { onMenu(entry) },
                    )
                    .padding(vertical = 10.dp),
            ) {
                AppIcon(entry = entry, repository = repository, size = 50.dp, iconShape = iconShape)
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
                )
            }
        }
        // Ряд короче пяти не растягиваем: значки должны стоять в тех же
        // колонках, что и при полном ряде.
        repeat(FREQUENT - apps.size) { Spacer(Modifier.weight(1f)) }
    }
}

/**
 * Человек из книги: карточка по нажатию, звонок и сообщение кнопками справа.
 *
 * Звоним через `ACTION_DIAL`: номер уезжает в набор, а не в вызов, поэтому
 * разрешение на звонки лаунчеру не нужно.
 */
@Composable
internal fun ContactRow(
    person: Contact,
    onOpen: () -> Unit,
    onCall: () -> Unit,
    onWrite: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (person.photo != null) {
                AsyncImage(
                    model = person.photo,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(
                    text = person.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = person.phone,
                fontFamily = MonoFont,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ContactAction(Icons.Rounded.Call, stringResource(R.string.contact_call), onCall)
        ContactAction(Icons.Rounded.Message, stringResource(R.string.contact_write), onWrite)
    }
}

@Composable
private fun ContactAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = MonoFont,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp, top = 14.dp, bottom = 6.dp),
    )
}

/** Карточка с ответом: слева запрос, справа результат. */
@Composable
internal fun ResultCard(left: String, right: String, note: String? = null) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = left,
                    fontFamily = MonoFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                note?.let {
                    Text(
                        text = it,
                        fontFamily = MonoFont,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Text(
                text = right,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** Та же карточка для снимков экрана. */
@Composable
fun SearchPreviewCard(left: String, right: String, note: String? = null) =
    ResultCard(left = left, right = right, note = note)
