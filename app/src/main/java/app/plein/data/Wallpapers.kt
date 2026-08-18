package app.plein.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Куда ставить кадр обоями. */
enum class WallpaperTarget { Home, Lock, Both }

/**
 * Кадр лаунчера обоями системы.
 *
 * Раздельные обои для рабочего стола и экрана блокировки появились в Android 7;
 * ниже система знает единственную картинку, поэтому любой выбор ставит её же.
 */
object Wallpapers {

    suspend fun set(context: Context, backdrop: Backdrop, target: WallpaperTarget): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = when {
                    backdrop.file != null -> BitmapFactory.decodeFile(backdrop.file.path)
                    backdrop.asset != null -> context.assets.open(backdrop.asset).use {
                        BitmapFactory.decodeStream(it)
                    }
                    else -> null
                } ?: return@withContext false

                val manager = WallpaperManager.getInstance(context)
                val flags = when (target) {
                    WallpaperTarget.Home -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.Lock -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.Both -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                manager.setBitmap(bitmap, null, true, flags)
                true
            }.getOrDefault(false)
        }
}
