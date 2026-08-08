package app.plein.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

/**
 * Паки значков в формате ADW и Nova.
 *
 * Пак это обычное приложение с `appfilter.xml`, где каждой активности
 * сопоставлено имя картинки. Читаем разметку из чужих ресурсов и берём
 * оттуда drawable — качать ничего не нужно, всё уже стоит на телефоне.
 */
class IconPacks(private val context: Context) {

    data class Pack(val packageName: String, val label: String)

    private val cache = HashMap<String, Map<String, String>>()

    /** Установленные паки: их объявляют через свои intent-фильтры. */
    fun installed(): List<Pack> {
        val pm = context.packageManager
        val actions = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME")
        val found = LinkedHashMap<String, Pack>()

        actions.forEach { action ->
            val activities = runCatching {
                pm.queryIntentActivities(Intent(action), PackageManager.MATCH_DEFAULT_ONLY)
            }.getOrDefault(emptyList())
            activities.forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg !in found) {
                    found[pkg] = Pack(pkg, info.loadLabel(pm).toString())
                }
            }
        }
        return found.values.sortedBy { it.label }
    }

    /** Значок из пака или null, если для этой активности картинки нет. */
    suspend fun icon(packPackage: String, component: ComponentName): Drawable? =
        withContext(Dispatchers.IO) {
            val mapping = cache.getOrPut(packPackage) { readAppFilter(packPackage) }
            val key = "ComponentInfo{${component.packageName}/${component.className}}"
            val drawableName = mapping[key] ?: return@withContext null

            runCatching {
                val resources = context.packageManager.getResourcesForApplication(packPackage)
                @Suppress("DiscouragedApi")
                val id = resources.getIdentifier(drawableName, "drawable", packPackage)
                if (id == 0) null else resources.getDrawable(id, null)
            }.getOrNull()
        }

    private fun readAppFilter(packPackage: String): Map<String, String> {
        val resources = runCatching {
            context.packageManager.getResourcesForApplication(packPackage)
        }.getOrNull() ?: return emptyMap()

        val parser = runCatching {
            @Suppress("DiscouragedApi")
            val id = resources.getIdentifier("appfilter", "xml", packPackage)
            if (id != 0) resources.getXml(id) else null
        }.getOrNull() ?: return readAppFilterFromAssets(packPackage)

        val mapping = HashMap<String, String>()
        runCatching {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) mapping[component] = drawable
                }
                event = parser.next()
            }
        }
        return mapping
    }

    /** Часть паков кладёт разметку в assets, а не в ресурсы. */
    private fun readAppFilterFromAssets(packPackage: String): Map<String, String> {
        val mapping = HashMap<String, String>()
        runCatching {
            val assets = context.packageManager.getResourcesForApplication(packPackage).assets
            assets.open("appfilter.xml").use { stream ->
                val parser = android.util.Xml.newPullParser()
                parser.setInput(stream, null)
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (component != null && drawable != null) mapping[component] = drawable
                    }
                    event = parser.next()
                }
            }
        }
        return mapping
    }
}
