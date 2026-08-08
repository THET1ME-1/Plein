package app.plein.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.SizeF

/**
 * Виджеты установленных приложений.
 *
 * Лаунчер обязан быть хостом: система не отдаёт чужую отрисовку кому попало.
 * Хост держит номера виджетов, слушает обновления, пока экран на виду, и молча
 * отпускает, когда лаунчер ушёл — иначе приложения продолжают рисовать в
 * пустоту и жгут батарею.
 *
 * Привязка идёт в два шага: сперва номер, потом разрешение. На большинстве
 * прошивок лаунчеру по умолчанию нельзя привязывать виджеты молча, и система
 * показывает свой запрос.
 */
class Widgets(private val context: Context) {

    private val manager = AppWidgetManager.getInstance(context)
    private val host = AppWidgetHost(context, HOST_ID)

    fun startListening() = runCatching { host.startListening() }

    fun stopListening() = runCatching { host.stopListening() }

    /** Всё, что можно поставить: по одному пункту на виджет приложения. */
    fun providers(): List<Provider> = runCatching {
        manager.installedProviders.map { info ->
            Provider(
                info = info,
                label = info.loadLabel(context.packageManager).orEmpty(),
                icon = runCatching { info.loadPreviewImage(context, 0) }.getOrNull()
                    ?: runCatching { info.loadIcon(context, 0) }.getOrNull(),
            )
        }.sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun release(widgetId: Int) {
        runCatching { host.deleteAppWidgetId(widgetId) }
    }

    /** Молчаливая привязка. Отказ означает, что нужен системный запрос. */
    fun bind(widgetId: Int, info: AppWidgetProviderInfo): Boolean = runCatching {
        manager.bindAppWidgetIdIfAllowed(widgetId, info.provider)
    }.getOrDefault(false)

    fun bindIntent(widgetId: Int, info: AppWidgetProviderInfo) =
        android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
        }

    fun infoOf(widgetId: Int): AppWidgetProviderInfo? =
        runCatching { manager.getAppWidgetInfo(widgetId) }.getOrNull()

    /** У виджета может быть свой экран настройки — без него он пустой. */
    fun needsConfigure(info: AppWidgetProviderInfo): Boolean = info.configure != null

    fun startConfigure(activity: android.app.Activity, widgetId: Int, requestCode: Int) {
        runCatching {
            host.startAppWidgetConfigureActivityForResult(activity, widgetId, 0, requestCode, null)
        }
    }

    fun createView(widgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView =
        host.createView(context, widgetId, info)

    /**
     * Сколько клеток просит виджет.
     *
     * Провайдер называет минимальные размеры в точках, а нам нужны клетки —
     * делим и округляем вверх, иначе виджет приедет обрезанным.
     */
    fun cellsFor(info: AppWidgetProviderInfo, cellWidthDp: Int, cellHeightDp: Int): Pair<Int, Int> {
        val width = (info.minWidth + cellWidthDp - 1) / cellWidthDp.coerceAtLeast(1)
        val height = (info.minHeight + cellHeightDp - 1) / cellHeightDp.coerceAtLeast(1)
        return width.coerceIn(1, 4) to height.coerceIn(1, 4)
    }

    /** Сообщаем виджету, сколько места ему дали: иначе он рисует наугад. */
    fun applySize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.updateAppWidgetSize(
                    Bundle(),
                    listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())),
                )
            } else {
                @Suppress("DEPRECATION")
                view.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
            }
        }
    }

    data class Provider(
        val info: AppWidgetProviderInfo,
        val label: String,
        val icon: Drawable?,
    )

    private companion object {
        /** Свой номер хоста: система различает хостов по нему. */
        const val HOST_ID = 0x504C
    }
}
